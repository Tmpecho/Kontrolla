import type { TemperatureLogEntry, TemperatureUnit } from '@/ik-mat/model/temperature.types'

function createTimestamp(daysAgo: number, hours: number, minutes: number): string {
  const timestamp = new Date()
  timestamp.setSeconds(0, 0)
  timestamp.setDate(timestamp.getDate() - daysAgo)
  timestamp.setHours(hours, minutes, 0, 0)
  return timestamp.toISOString()
}

function createLogEntry(
  id: string,
  daysAgo: number,
  hours: number,
  minutes: number,
  temperatureCelsius: number,
  note: string | null = null,
): TemperatureLogEntry {
  return {
    id,
    measuredAt: createTimestamp(daysAgo, hours, minutes),
    temperatureCelsius,
    note,
  }
}

export function createTemperatureUnits(): TemperatureUnit[] {
  return [
    {
      id: 'unit-sushi-fridge',
      name: 'Sushi prep fridge',
      location: 'Hot kitchen',
      type: 'FRIDGE',
      minimumTemperature: 2,
      maximumTemperature: 4,
      logs: [
        createLogEntry('sushi-fridge-1', 0, 8, 10, 3.2, 'Morning opening check completed.'),
        createLogEntry('sushi-fridge-2', 1, 8, 5, 3.4),
        createLogEntry('sushi-fridge-3', 2, 8, 8, 3.1),
        createLogEntry('sushi-fridge-4', 3, 8, 4, 3.3),
        createLogEntry('sushi-fridge-5', 4, 8, 9, 3.5),
        createLogEntry('sushi-fridge-6', 5, 8, 11, 3.4),
        createLogEntry('sushi-fridge-7', 6, 8, 6, 3.2),
      ],
    },
    {
      id: 'unit-walk-in-cooler',
      name: 'Walk-in cooler',
      location: 'Receiving room',
      type: 'FRIDGE',
      minimumTemperature: 0,
      maximumTemperature: 4,
      logs: [
        createLogEntry('walk-in-1', 0, 8, 20, 5.7, 'Door was found slightly ajar.'),
        createLogEntry('walk-in-2', 1, 8, 16, 3.8),
        createLogEntry('walk-in-3', 2, 8, 17, 3.4),
        createLogEntry('walk-in-4', 3, 8, 10, 3.6),
        createLogEntry('walk-in-5', 4, 8, 13, 3.5),
        createLogEntry('walk-in-6', 5, 8, 14, 3.8),
        createLogEntry('walk-in-7', 6, 8, 12, 3.7),
      ],
    },
    {
      id: 'unit-dessert-freezer',
      name: 'Dessert freezer',
      location: 'Cold dessert station',
      type: 'FREEZER',
      minimumTemperature: -23,
      maximumTemperature: -18,
      logs: [
        createLogEntry('dessert-freezer-1', 1, 20, 5, -20.6, 'Evening close completed.'),
        createLogEntry('dessert-freezer-2', 2, 20, 2, -20.4),
        createLogEntry('dessert-freezer-3', 3, 20, 4, -20.8),
        createLogEntry('dessert-freezer-4', 4, 20, 1, -20.2),
        createLogEntry('dessert-freezer-5', 5, 20, 0, -20.5),
        createLogEntry('dessert-freezer-6', 6, 20, 3, -20.1),
        createLogEntry('dessert-freezer-7', 7, 20, 6, -20.3),
      ],
    },
    {
      id: 'unit-bar-fridge',
      name: 'Bar garnish fridge',
      location: 'Front bar',
      type: 'FRIDGE',
      minimumTemperature: 2,
      maximumTemperature: 5,
      logs: [
        createLogEntry('bar-fridge-1', 0, 10, 0, 4.4),
        createLogEntry('bar-fridge-2', 1, 10, 3, 4.6),
        createLogEntry('bar-fridge-3', 2, 10, 5, 4.3),
        createLogEntry('bar-fridge-4', 3, 10, 2, 4.7),
        createLogEntry('bar-fridge-5', 4, 10, 4, 4.4),
        createLogEntry('bar-fridge-6', 5, 10, 6, 4.5),
        createLogEntry('bar-fridge-7', 6, 10, 1, 4.2),
      ],
    },
    {
      id: 'unit-frozen-storage',
      name: 'Frozen storage A',
      location: 'Basement freezer room',
      type: 'FREEZER',
      minimumTemperature: -24,
      maximumTemperature: -18,
      logs: [
        createLogEntry('frozen-storage-1', 0, 7, 45, -19.5),
        createLogEntry('frozen-storage-2', 1, 7, 46, -19.7),
        createLogEntry('frozen-storage-3', 2, 7, 44, -19.3),
        createLogEntry('frozen-storage-4', 3, 7, 43, -19.8),
        createLogEntry('frozen-storage-5', 4, 7, 45, -19.6),
        createLogEntry('frozen-storage-6', 5, 7, 47, -19.4),
        createLogEntry('frozen-storage-7', 6, 7, 42, -19.5),
      ],
    },
  ]
}
