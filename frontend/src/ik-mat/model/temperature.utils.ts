import type {
  TemperatureLogEntry,
  TemperatureStatus,
  TemperatureUnit,
  TemperatureUnitListItem,
  TemperatureUnitType,
} from '@/ik-mat/model/temperature.types'

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

export function hasTemperatureLogToday(unit: TemperatureUnit, now: Date = new Date()): boolean {
  return unit.logs.some((logEntry) => isSameLocalDate(new Date(logEntry.measuredAt), now))
}

export function getTemperatureStatus(
  unit: TemperatureUnit,
  now: Date = new Date(),
): TemperatureStatus {
  if (!hasTemperatureLogToday(unit, now)) {
    return 'OVERDUE'
  }

  const latestLog = getLatestTemperatureLog(unit)

  if (!latestLog) {
    return 'OVERDUE'
  }

  if (
    latestLog.temperatureCelsius < unit.minimumTemperature ||
    latestLog.temperatureCelsius > unit.maximumTemperature
  ) {
    return 'OUT_OF_RANGE'
  }

  return 'IN_RANGE'
}

export function getTemperatureUnitsWithStatus(
  units: TemperatureUnit[],
  now: Date = new Date(),
): TemperatureUnitListItem[] {
  return units.map((unit) => ({
    ...unit,
    latestLog: getLatestTemperatureLog(unit),
    status: getTemperatureStatus(unit, now),
  }))
}

export function getTemperatureSummary(units: TemperatureUnit[], now: Date = new Date()) {
  const unitsWithStatus = getTemperatureUnitsWithStatus(units, now)

  return {
    needsAttentionCount: unitsWithStatus.filter((unit) => unit.status !== 'IN_RANGE').length,
    dueTodayCount: unitsWithStatus.filter((unit) => unit.status === 'OVERDUE').length,
    inRangeCount: unitsWithStatus.filter((unit) => unit.status === 'IN_RANGE').length,
  }
}

export function formatTemperatureStatus(status: TemperatureStatus): string {
  switch (status) {
    case 'OUT_OF_RANGE':
      return 'Out of range'
    case 'OVERDUE':
      return 'Due today'
    default:
      return 'In range'
  }
}

export function formatTemperatureUnitType(type: TemperatureUnitType): string {
  return type === 'FREEZER' ? 'Freezer' : 'Fridge'
}
