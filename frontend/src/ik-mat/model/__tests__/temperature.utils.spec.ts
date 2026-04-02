import { describe, expect, it, vi } from 'vitest'

import {
  formatTemperatureStatus,
  getTemperatureStatus,
  getTemperatureSummary,
  getTemperatureUnitsWithStatus,
  hasTemperatureLogToday,
} from '@/ik-mat/model/temperature.utils'
import type { TemperatureUnit } from '@/ik-mat/model/temperature.types'

function createTemperatureUnit(
  id: string,
  latestMeasuredAt: string,
  temperatureCelsius: number,
): TemperatureUnit {
  return {
    id,
    name: `${id} unit`,
    location: `${id} location`,
    type: 'FRIDGE',
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs: [
      {
        id: `${id}-log`,
        measuredAt: latestMeasuredAt,
        temperatureCelsius,
        note: null,
      },
    ],
  }
}

describe('temperature.utils', () => {
  it('detects whether a unit has been logged today', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 10, 0, 0))

    const loggedToday = createTemperatureUnit('today', '2026-04-02T08:00:00.000Z', 3.4)
    const notLoggedToday = createTemperatureUnit('yesterday', '2026-04-01T08:00:00.000Z', 3.4)

    expect(hasTemperatureLogToday(loggedToday)).toBe(true)
    expect(hasTemperatureLogToday(notLoggedToday)).toBe(false)

    vi.useRealTimers()
  })

  it('classifies in-range, out-of-range, and due-today units', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 10, 0, 0))

    expect(getTemperatureStatus(createTemperatureUnit('in-range', '2026-04-02T08:00:00.000Z', 3.4))).toBe(
      'IN_RANGE',
    )
    expect(
      getTemperatureStatus(createTemperatureUnit('out-of-range', '2026-04-02T08:00:00.000Z', 5.2)),
    ).toBe('OUT_OF_RANGE')
    expect(getTemperatureStatus(createTemperatureUnit('due', '2026-04-01T08:00:00.000Z', 3.4))).toBe(
      'OVERDUE',
    )

    vi.useRealTimers()
  })

  it('builds summary counts from the unit list', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 2, 10, 0, 0))

    const units = [
      createTemperatureUnit('in-range', '2026-04-02T08:00:00.000Z', 3.4),
      createTemperatureUnit('out-of-range', '2026-04-02T08:00:00.000Z', 5.2),
      createTemperatureUnit('due', '2026-04-01T08:00:00.000Z', 3.4),
    ]

    expect(getTemperatureSummary(units)).toEqual({
      needsAttentionCount: 2,
      dueTodayCount: 1,
      inRangeCount: 1,
    })

    expect(getTemperatureUnitsWithStatus(units).map((unit) => unit.status)).toEqual([
      'IN_RANGE',
      'OUT_OF_RANGE',
      'OVERDUE',
    ])

    vi.useRealTimers()
  })

  it('formats the status labels used in the UI', () => {
    expect(formatTemperatureStatus('IN_RANGE')).toBe('In range')
    expect(formatTemperatureStatus('OUT_OF_RANGE')).toBe('Out of range')
    expect(formatTemperatureStatus('OVERDUE')).toBe('Due today')
  })
})
