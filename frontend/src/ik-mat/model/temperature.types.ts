export type TemperatureUnitType = 'FRIDGE' | 'FREEZER'

export type TemperatureStatus = 'IN_RANGE' | 'OUT_OF_RANGE' | 'OVERDUE'

export type TemperatureLogEntry = {
  id: string
  measuredAt: string
  temperatureCelsius: number
  note: string | null
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
  status: TemperatureStatus
  hasLoggedToday: boolean
}
