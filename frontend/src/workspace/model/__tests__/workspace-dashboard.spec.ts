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

function createLocalIsoTimestamp(
  year: number,
  monthIndex: number,
  day: number,
  hours: number,
  minutes: number,
): string {
  return new Date(year, monthIndex, day, hours, minutes, 0, 0).toISOString()
}

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
    establishmentId: 'est-1',
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
    auditAssignments: [],
  }
}

describe('workspace-dashboard', () => {
  it('builds the service summaries from current feature data', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 3, 5, 10, 0, 0))

    const checklistRuns = [
      createChecklistRun('overdue-run', 'OVERDUE', createLocalIsoTimestamp(2026, 3, 5, 7, 0)),
      createChecklistRun('active-run', 'IN_PROGRESS', createLocalIsoTimestamp(2026, 3, 5, 12, 0)),
      createChecklistRun('complete-run', 'COMPLETED', createLocalIsoTimestamp(2026, 3, 4, 12, 0)),
    ]
    const temperatureUnits = [
      createTemperatureUnit({
        id: 'walk-in',
        dueByTime: '08:30',
        measuredAt: createLocalIsoTimestamp(2026, 3, 5, 8, 10),
        temperatureCelsius: 5.3,
      }),
      createTemperatureUnit({
        id: 'dessert',
        dueByTime: '20:30',
        measuredAt: createLocalIsoTimestamp(2026, 3, 5, 8, 0),
        temperatureCelsius: 3.4,
      }),
    ]
    const ikMatDeviations = [
      createDeviation('food-open', 'IK_MAT', 'HIGH', 'OPEN', createLocalIsoTimestamp(2026, 3, 5, 9, 15)),
      createDeviation(
        'food-resolved',
        'IK_MAT',
        'LOW',
        'RESOLVED',
        createLocalIsoTimestamp(2026, 3, 4, 9, 15),
      ),
    ]
    const ikAlkoholDeviations = [
      createDeviation(
        'alcohol-open',
        'IK_ALKOHOL',
        'CRITICAL',
        'OPEN',
        createLocalIsoTimestamp(2026, 3, 5, 0, 15),
      ),
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
        documents: [
          createDocumentRecord('expired', '2026-04-04'),
          createDocumentRecord('expiring', '2026-04-15'),
          {
            ...createDocumentRecord('valid', '2026-06-20'),
            auditAssignments: [
              {
                userId: 'user-1',
                userEmail: 'reader@example.com',
                userFirstName: 'Reader',
                userLastName: 'One',
                acknowledgedAt: null,
              },
            ],
          },
        ],
        deviations: ikAlkoholDeviations,
        currentUserId: 'user-1',
        note: 'Deviation overview is temporarily unavailable.',
      }).metrics,
    ).toEqual([
      { label: 'Open alcohol deviations', value: '1 item', tone: 'critical' },
      { label: 'Documents needing attention', value: '2 documents', tone: 'warning' },
      { label: 'Needs your audit', value: '1 document', tone: 'primary' },
    ])

    expect(
      buildIKAlkoholServiceSummary({
        documents,
        deviations: ikAlkoholDeviations,
        currentUserId: 'user-1',
        note: 'Deviation overview is temporarily unavailable.',
      }).note,
    ).toBe('Deviation overview is temporarily unavailable.')

    vi.useRealTimers()
  })

  it('orders attention items by operational urgency across services', () => {
    const checklistRuns = [
      createChecklistRun('late-open', 'OVERDUE', createLocalIsoTimestamp(2026, 3, 5, 7, 0)),
    ]
    const temperatureUnits = [
      createTemperatureUnit({
        id: 'walk-in',
        dueByTime: '08:30',
        measuredAt: createLocalIsoTimestamp(2026, 3, 5, 8, 20),
        temperatureCelsius: 5.7,
      }),
      createTemperatureUnit({
        id: 'dessert',
        dueByTime: '10:30',
        measuredAt: createLocalIsoTimestamp(2026, 3, 4, 20, 0),
        temperatureCelsius: 3.2,
      }),
    ]
    const deviationsByService: Record<'IK_MAT' | 'IK_ALKOHOL', DeviationListItem[]> = {
      IK_MAT: [],
      IK_ALKOHOL: [
        createDeviation(
          'alcohol-critical',
          'IK_ALKOHOL',
          'CRITICAL',
          'OPEN',
          createLocalIsoTimestamp(2026, 3, 5, 0, 18),
        ),
      ],
    }
    const documents = [createDocumentRecord('expired-licence', '2026-04-04')]

    const items = buildWorkspaceAttentionItems({
      checklistRuns,
      temperatureUnits,
      deviationsByService,
      documents,
      now: new Date(2026, 3, 5, 9, 0, 0),
    })

    expect(items.map((item) => item.id)).toEqual([
      'deviation-alcohol-critical',
      'temperature-walk-in',
      'checklist-late-open',
      'document-expired-licence',
      'temperature-dessert',
    ])
  })

  it('renders placeholders and skips unavailable document and temperature attention data', () => {
    const ikMatSummary = buildIKMatServiceSummary({
      checklistRuns: null,
      temperatureUnits: null,
      deviations: [],
    })

    const ikAlkoholSummary = buildIKAlkoholServiceSummary({
      documents: null,
      deviations: [],
      currentUserId: 'user-1',
    })

    expect(ikMatSummary.metrics).toEqual([
      { label: 'Active checklist runs', value: '—', tone: 'neutral' },
      { label: 'Temperature units needing attention', value: '—', tone: 'neutral' },
      { label: 'Open food deviations', value: '0 items', tone: 'neutral' },
    ])

    expect(ikAlkoholSummary.metrics).toEqual([
      { label: 'Open alcohol deviations', value: '0 items', tone: 'neutral' },
      { label: 'Documents needing attention', value: '—', tone: 'neutral' },
      { label: 'Needs your audit', value: '—', tone: 'neutral' },
    ])

    const items = buildWorkspaceAttentionItems({
      checklistRuns: [],
      temperatureUnits: null,
      deviationsByService: {
        IK_MAT: [],
        IK_ALKOHOL: [],
      },
      documents: null,
      now: new Date(2026, 3, 5, 9, 0, 0),
    })

    expect(items).toEqual([])
  })
})
