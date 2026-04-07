import { describe, expect, it, vi } from 'vitest'

import type { ChecklistRun } from '@/checklists/model/checklist.types'
import type { DeviationListItem } from '@/deviations/model/deviation.types'
import type { ImportantDocumentRecord } from '@/ik-alkohol/model/document.types'
import type { TemperatureUnit } from '@/ik-mat/model/temperature.types'
import {
  buildIKAlkoholServiceSummary,
  buildIKMatServiceSummary,
  buildWorkspaceAttentionItems,
} from '@/workspace/model/workspace-dashboard'

function createChecklistRun(id: string, status: ChecklistRun['status'], dueAt: string): ChecklistRun {
  return {
    id,
    checklistDefinitionId: `definition-${id}`,
    definitionGroupId: `group-${id}`,
    establishmentId: 'est-1',
    serviceArea: 'IK_MAT',
    title: `${id} checklist`,
    description: null,
    dueAt,
    status,
    startedAt: null,
    completedAt: null,
    completedByUserId: null,
    createdByUserId: 'user-1',
    createdAt: dueAt,
    updatedAt: dueAt,
    assignments: [],
    tasks: [],
    events: [],
  }
}

function createTemperatureUnit({
  id,
  dueByTime,
  measuredAt,
  temperatureCelsius,
}: {
  id: string
  dueByTime: string
  measuredAt: string
  temperatureCelsius: number
}): TemperatureUnit {
  return {
    id,
    name: id,
    location: `${id} location`,
    type: 'FRIDGE',
    dueByTime,
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs: [
      {
        id: `${id}-log`,
        measuredAt,
        temperatureCelsius,
        note: null,
        loggedByName: 'Maria Nilsen',
      },
    ],
  }
}

function createDeviation(
  id: string,
  serviceArea: DeviationListItem['serviceArea'],
  severity: DeviationListItem['severity'],
  status: DeviationListItem['status'],
  reportedAt: string,
): DeviationListItem {
  return {
    id,
    serviceArea,
    title: `${id} deviation`,
    reportedAt,
    category: serviceArea === 'IK_MAT' ? 'Temperature' : 'Serving hours',
    severity,
    status,
    assignedToUserId: 'user-1',
    assignedTo: ['Nora Johansen'],
    description: `${id} description`,
    timeline: [],
  }
}

function createDocumentRecord(id: string, renewalDate: string): ImportantDocumentRecord {
  return {
    id,
    title: `${id} document`,
    holderName: `${id} holder`,
    issueDate: '2026-01-01',
    renewalDate,
  }
}

describe('workspace-dashboard', () => {
  it('builds the service summaries from current feature data', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 5, 10, 0, 0))

    const checklistRuns = [
      createChecklistRun('overdue-run', 'OVERDUE', '2026-04-05T07:00:00+02:00'),
      createChecklistRun('active-run', 'IN_PROGRESS', '2026-04-05T12:00:00+02:00'),
      createChecklistRun('complete-run', 'COMPLETED', '2026-04-04T12:00:00+02:00'),
    ]
    const temperatureUnits = [
      createTemperatureUnit({
        id: 'walk-in',
        dueByTime: '08:30',
        measuredAt: '2026-04-05T08:10:00+02:00',
        temperatureCelsius: 5.3,
      }),
      createTemperatureUnit({
        id: 'dessert',
        dueByTime: '20:30',
        measuredAt: '2026-04-05T08:00:00+02:00',
        temperatureCelsius: 3.4,
      }),
    ]
    const ikMatDeviations = [
      createDeviation('food-open', 'IK_MAT', 'HIGH', 'OPEN', '2026-04-05T09:15:00+02:00'),
      createDeviation('food-resolved', 'IK_MAT', 'LOW', 'RESOLVED', '2026-04-04T09:15:00+02:00'),
    ]
    const ikAlkoholDeviations = [
      createDeviation('alcohol-open', 'IK_ALKOHOL', 'CRITICAL', 'OPEN', '2026-04-05T00:15:00+02:00'),
    ]
    const documents = [
      createDocumentRecord('expired', '2026-04-04'),
      createDocumentRecord('expiring', '2026-04-15'),
      createDocumentRecord('valid', '2026-06-20'),
    ]

    expect(
      buildIKMatServiceSummary({
        checklistRuns,
        temperatureUnits,
        deviations: ikMatDeviations,
      }).metrics,
    ).toEqual([
      { label: 'Active checklist runs', value: '2 runs', tone: 'primary' },
      {
        label: 'Temperature units needing attention',
        value: '1 unit',
        tone: 'warning',
      },
      {
        label: 'Open food deviations',
        value: '1 item',
        tone: 'critical',
      },
    ])

    expect(
      buildIKAlkoholServiceSummary({
        documents,
        deviations: ikAlkoholDeviations,
      }).metrics,
    ).toEqual([
      { label: 'Open alcohol deviations', value: '1 item', tone: 'critical' },
      { label: 'Documents needing attention', value: '2 documents', tone: 'warning' },
      { label: 'Audit readiness', value: '67%', tone: 'primary' },
    ])

    vi.useRealTimers()
  })

  it('orders attention items by operational urgency across services', () => {
    const checklistRuns = [createChecklistRun('late-open', 'OVERDUE', '2026-04-05T07:00:00+02:00')]
    const temperatureUnits = [
      createTemperatureUnit({
        id: 'walk-in',
        dueByTime: '08:30',
        measuredAt: '2026-04-05T08:20:00+02:00',
        temperatureCelsius: 5.7,
      }),
      createTemperatureUnit({
        id: 'dessert',
        dueByTime: '10:30',
        measuredAt: '2026-04-04T20:00:00+02:00',
        temperatureCelsius: 3.2,
      }),
    ]
    const deviationsByService: Record<'IK_MAT' | 'IK_ALKOHOL', DeviationListItem[]> = {
      IK_MAT: [],
      IK_ALKOHOL: [
        createDeviation('alcohol-critical', 'IK_ALKOHOL', 'CRITICAL', 'OPEN', '2026-04-05T00:18:00+02:00'),
      ],
    }
    const documents = [createDocumentRecord('expired-licence', '2026-04-04')]

    const items = buildWorkspaceAttentionItems({
      checklistRuns,
      temperatureUnits,
      deviationsByService,
      documents,
      now: new Date('2026-04-05T09:00:00+02:00'),
    })

    expect(items.map((item) => item.id)).toEqual([
      'deviation-alcohol-critical',
      'temperature-walk-in',
      'checklist-late-open',
      'document-expired-licence',
      'temperature-dessert',
    ])
  })
})
