import { describe, expect, it, vi } from 'vitest'

import {
  formatTemperatureAlertState,
  getTemperatureAlertState,
  getTemperatureComplianceStatus,
  getTemperatureLoggingStatus,
  getTemperatureSummary,
  getTemperatureUnitsWithStatus,
  hasTemperatureLogToday,
  isTemperatureWithinRange,
} from '@/ik-mat/model/temperature.utils'
import type { TemperatureUnit } from '@/ik-mat/model/temperature.types'

function createLocalIsoTimestamp(
  year: number,
  monthIndex: number,
  day: number,
  hours: number,
  minutes: number,
): string {
  return new Date(year, monthIndex, day, hours, minutes, 0, 0).toISOString()
}

function createTemperatureUnit({
  id,
  dueByTime = '09:00',
  measuredAt,
  temperatureCelsius,
}: {
  id: string
  dueByTime?: string
  measuredAt?: string
  temperatureCelsius?: number
}): TemperatureUnit {
  return {
    id,
    name: `${id} unit`,
    location: `${id} location`,
    type: 'FRIDGE',
    dueByTime,
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs:
      measuredAt && typeof temperatureCelsius === 'number'
        ? [
            {
              id: `${id}-log`,
              measuredAt,
              temperatureCelsius,
              note: null,
              loggedByName: 'Maria Nilsen',
            },
          ]
        : [],
  }
}

describe('temperature.utils', () => {
  it('detects whether a unit has been logged today', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 12, 0, 0))

    const loggedToday = createTemperatureUnit({
      id: 'today',
      measuredAt: createLocalIsoTimestamp(2026, 3, 2, 8, 0),
      temperatureCelsius: 3.4,
    })
    const notLoggedToday = createTemperatureUnit({
      id: 'yesterday',
      measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
      temperatureCelsius: 3.4,
    })

    expect(hasTemperatureLogToday(loggedToday)).toBe(true)
    expect(hasTemperatureLogToday(notLoggedToday)).toBe(false)

    vi.useRealTimers()
  })

  it('distinguishes due later today, due soon, and overdue states', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 12, 0, 0))

    const dueLater = createTemperatureUnit({
      id: 'due-later',
      dueByTime: '17:00',
      measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
      temperatureCelsius: 3.4,
    })
    const dueSoon = createTemperatureUnit({
      id: 'due-soon',
      dueByTime: '13:30',
      measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
      temperatureCelsius: 3.4,
    })
    const overdue = createTemperatureUnit({
      id: 'overdue',
      dueByTime: '10:00',
      measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
      temperatureCelsius: 3.4,
    })

    expect(getTemperatureLoggingStatus(dueLater)).toBe('DUE_LATER_TODAY')
    expect(getTemperatureLoggingStatus(dueSoon)).toBe('DUE_SOON')
    expect(getTemperatureLoggingStatus(overdue)).toBe('OVERDUE')

    vi.useRealTimers()
  })

  it('treats out-of-range as the primary alert even when the unit is already logged today', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 12, 0, 0))

    const inRange = createTemperatureUnit({
      id: 'in-range',
      measuredAt: createLocalIsoTimestamp(2026, 3, 2, 8, 0),
      temperatureCelsius: 3.4,
    })
    const outOfRange = createTemperatureUnit({
      id: 'out-of-range',
      measuredAt: createLocalIsoTimestamp(2026, 3, 2, 8, 0),
      temperatureCelsius: 5.2,
    })

    expect(isTemperatureWithinRange(inRange, 3.4)).toBe(true)
    expect(isTemperatureWithinRange(outOfRange, 5.2)).toBe(false)
    expect(getTemperatureComplianceStatus(inRange)).toBe('IN_RANGE')
    expect(getTemperatureComplianceStatus(outOfRange)).toBe('OUT_OF_RANGE')
    expect(getTemperatureAlertState(inRange)).toBe('LOGGED_TODAY')
    expect(getTemperatureAlertState(outOfRange)).toBe('OUT_OF_RANGE')

    vi.useRealTimers()
  })

  it('builds the derived list and summary counts from the unit list', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 12, 0, 0))

    const units = [
      createTemperatureUnit({
        id: 'in-range',
        measuredAt: createLocalIsoTimestamp(2026, 3, 2, 8, 0),
        temperatureCelsius: 3.4,
      }),
      createTemperatureUnit({
        id: 'out-of-range',
        measuredAt: createLocalIsoTimestamp(2026, 3, 2, 8, 0),
        temperatureCelsius: 5.2,
      }),
      createTemperatureUnit({
        id: 'due-soon',
        dueByTime: '13:00',
        measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
        temperatureCelsius: 3.4,
      }),
      createTemperatureUnit({
        id: 'overdue',
        dueByTime: '09:00',
        measuredAt: createLocalIsoTimestamp(2026, 3, 1, 8, 0),
        temperatureCelsius: 3.4,
      }),
    ]

    expect(getTemperatureSummary(units)).toEqual({
      needsAttentionCount: 3,
      overdueNowCount: 1,
      dueSoonCount: 1,
      latestInRangeCount: 3,
    })

    expect(getTemperatureUnitsWithStatus(units).map((unit) => unit.alertState)).toEqual([
      'LOGGED_TODAY',
      'OUT_OF_RANGE',
      'DUE_SOON',
      'OVERDUE',
    ])

    vi.useRealTimers()
  })

  it('formats the alert labels used in the UI', () => {
    expect(formatTemperatureAlertState('LOGGED_TODAY')).toBe('Logged today')
    expect(formatTemperatureAlertState('OUT_OF_RANGE')).toBe('Out of range')
    expect(formatTemperatureAlertState('OVERDUE')).toBe('Overdue')
    expect(formatTemperatureAlertState('DUE_SOON')).toBe('Due soon')
    expect(formatTemperatureAlertState('DUE_LATER_TODAY')).toBe('Due later today')
    expect(formatTemperatureAlertState('NO_READING')).toBe('No reading yet')
  })
})
