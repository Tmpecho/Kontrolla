import type {
  TemperatureAlertState,
  TemperatureComplianceStatus,
  TemperatureLogEntry,
  TemperatureLoggingStatus,
  TemperatureUnit,
  TemperatureUnitListItem,
  TemperatureUnitType,
} from '@/ik-mat/model/temperature.types'

const DUE_SOON_WINDOW_IN_MINUTES = 120

export function getLatestTemperatureLog(unit: TemperatureUnit): TemperatureLogEntry | null {
  if (unit.logs.length === 0) {
    return null
  }

  return (
    [...unit.logs].sort(
      (left, right) => new Date(right.measuredAt).getTime() - new Date(left.measuredAt).getTime(),
    )[0] ?? null
  )
}

function isSameLocalDate(left: Date, right: Date): boolean {
  return (
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()
  )
}

function getDueDate(unit: TemperatureUnit, now: Date): Date {
  const [hours, minutes] = unit.dueByTime.split(':').map(Number)
  const dueDate = new Date(now)

  dueDate.setHours(hours ?? 0, minutes ?? 0, 0, 0)

  return dueDate
}

export function hasTemperatureLogToday(unit: TemperatureUnit, now: Date = new Date()): boolean {
  return unit.logs.some((logEntry) => isSameLocalDate(new Date(logEntry.measuredAt), now))
}

export function isTemperatureWithinRange(unit: TemperatureUnit, temperatureCelsius: number): boolean {
  return (
    temperatureCelsius >= unit.minimumTemperature && temperatureCelsius <= unit.maximumTemperature
  )
}

export function getTemperatureComplianceStatus(
  unit: TemperatureUnit,
): TemperatureComplianceStatus {
  const latestLog = getLatestTemperatureLog(unit)

  if (!latestLog) {
    return 'NO_READING'
  }

  if (!isTemperatureWithinRange(unit, latestLog.temperatureCelsius)) {
    return 'OUT_OF_RANGE'
  }

  return 'IN_RANGE'
}

export function getTemperatureLoggingStatus(
  unit: TemperatureUnit,
  now: Date = new Date(),
): TemperatureLoggingStatus {
  if (hasTemperatureLogToday(unit, now)) {
    return 'LOGGED_TODAY'
  }

  const dueDate = getDueDate(unit, now)
  const minutesUntilDue = (dueDate.getTime() - now.getTime()) / 60000

  if (minutesUntilDue < 0) {
    return 'OVERDUE'
  }

  if (minutesUntilDue <= DUE_SOON_WINDOW_IN_MINUTES) {
    return 'DUE_SOON'
  }

  return 'DUE_LATER_TODAY'
}

export function getTemperatureAlertState(
  unit: TemperatureUnit,
  now: Date = new Date(),
): TemperatureAlertState {
  const complianceStatus = getTemperatureComplianceStatus(unit)

  if (complianceStatus === 'OUT_OF_RANGE') {
    return 'OUT_OF_RANGE'
  }

  const loggingStatus = getTemperatureLoggingStatus(unit, now)

  switch (loggingStatus) {
    case 'OVERDUE':
      return 'OVERDUE'
    case 'DUE_SOON':
      return 'DUE_SOON'
    case 'DUE_LATER_TODAY':
      return complianceStatus === 'NO_READING' ? 'NO_READING' : 'DUE_LATER_TODAY'
    default:
      return complianceStatus === 'NO_READING' ? 'NO_READING' : 'LOGGED_TODAY'
  }
}

export function getTemperatureUnitsWithStatus(
  units: TemperatureUnit[],
  now: Date = new Date(),
): TemperatureUnitListItem[] {
  return units.map((unit) => ({
    ...unit,
    latestLog: getLatestTemperatureLog(unit),
    complianceStatus: getTemperatureComplianceStatus(unit),
    loggingStatus: getTemperatureLoggingStatus(unit, now),
    alertState: getTemperatureAlertState(unit, now),
    hasLoggedToday: hasTemperatureLogToday(unit, now),
    nextDueAt: getDueDate(unit, now),
  }))
}

export function getTemperatureSummary(units: TemperatureUnit[], now: Date = new Date()) {
  const unitsWithStatus = getTemperatureUnitsWithStatus(units, now)

  return {
    needsAttentionCount: unitsWithStatus.filter((unit) =>
      ['OUT_OF_RANGE', 'OVERDUE', 'DUE_SOON'].includes(unit.alertState),
    ).length,
    overdueNowCount: unitsWithStatus.filter((unit) => unit.loggingStatus === 'OVERDUE').length,
    dueSoonCount: unitsWithStatus.filter((unit) => unit.loggingStatus === 'DUE_SOON').length,
    latestInRangeCount: unitsWithStatus.filter((unit) => unit.complianceStatus === 'IN_RANGE')
      .length,
  }
}

export function formatTemperatureAlertState(state: TemperatureAlertState): string {
  switch (state) {
    case 'OUT_OF_RANGE':
      return 'Out of range'
    case 'OVERDUE':
      return 'Overdue'
    case 'DUE_SOON':
      return 'Due soon'
    case 'DUE_LATER_TODAY':
      return 'Due later today'
    case 'NO_READING':
      return 'No reading yet'
    default:
      return 'Logged today'
  }
}

export function formatTemperatureUnitType(type: TemperatureUnitType): string {
  return type === 'FREEZER' ? 'Freezer' : 'Fridge'
}
