export type TemperatureUnitType = 'FRIDGE' | 'FREEZER'

export type TemperatureComplianceStatus = 'NO_READING' | 'IN_RANGE' | 'OUT_OF_RANGE'

export type TemperatureLoggingStatus =
  | 'LOGGED_TODAY'
  | 'DUE_LATER_TODAY'
  | 'DUE_SOON'
  | 'OVERDUE'

export type TemperatureAlertState =
  | 'NO_READING'
  | 'OUT_OF_RANGE'
  | 'OVERDUE'
  | 'DUE_SOON'
  | 'DUE_LATER_TODAY'
  | 'LOGGED_TODAY'

export type TemperatureLogEntry = {
  id: string
  measuredAt: string
  temperatureCelsius: number
  note: string | null
  loggedByName: string
}

export type TemperatureUnit = {
  id: string
  name: string
  location: string
  type: TemperatureUnitType
  dueByTime: string
  minimumTemperature: number
  maximumTemperature: number
  logs: TemperatureLogEntry[]
}

export type TemperatureUnitListItem = TemperatureUnit & {
  latestLog: TemperatureLogEntry | null
  complianceStatus: TemperatureComplianceStatus
  loggingStatus: TemperatureLoggingStatus
  alertState: TemperatureAlertState
  hasLoggedToday: boolean
  nextDueAt: Date
}
