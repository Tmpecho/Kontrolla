import type { ChecklistRun } from '@/checklists/model/checklist.types'
import type { DeviationListItem, DeviationServiceArea, DeviationSeverity, DeviationStatus } from '@/deviations/model/deviation.types'
import type { ImportantDocumentRecord } from '@/ik-alkohol/model/document.types'
import { expiryWarningDays, getDocumentsWithStatus, parseLocalDate } from '@/ik-alkohol/model/document.utils'
import type { TemperatureAlertState, TemperatureUnit } from '@/ik-mat/model/temperature.types'
import {
  getTemperatureSummary,
  getTemperatureUnitsWithStatus,
} from '@/ik-mat/model/temperature.utils'

export type WorkspaceRouteTarget = {
  name: string
  query?: Record<string, string>
}

export type WorkspaceServiceKey = 'ik-mat' | 'ik-alkohol'

export type WorkspaceServiceMetricTone = 'neutral' | 'primary' | 'warning' | 'critical'

export type WorkspaceServiceMetric = {
  label: string
  value: string
  tone?: WorkspaceServiceMetricTone
}

export type WorkspaceServiceSummary = {
  key: WorkspaceServiceKey
  title: string
  description: string
  to: WorkspaceRouteTarget
  ctaLabel: string
  metrics: WorkspaceServiceMetric[]
  note?: string | null
}

export type WorkspaceAttentionTone = 'primary' | 'warning' | 'critical'

export type WorkspaceAttentionItem = {
  id: string
  title: string
  serviceLabel: string
  reason: string
  to: WorkspaceRouteTarget
  tone: WorkspaceAttentionTone
  priority: number
  sortAt: number
}

type BuildIKMatServiceSummaryOptions = {
  checklistRuns: ChecklistRun[] | null
  checklistNote?: string | null
  temperatureUnits: TemperatureUnit[]
  deviations: DeviationListItem[]
  now?: Date
}

type BuildIKAlkoholServiceSummaryOptions = {
  documents: ImportantDocumentRecord[]
  deviations: DeviationListItem[]
  note?: string | null
}

type BuildWorkspaceAttentionItemsOptions = {
  checklistRuns: ChecklistRun[]
  temperatureUnits: TemperatureUnit[]
  deviationsByService: Record<DeviationServiceArea, DeviationListItem[]>
  documents: ImportantDocumentRecord[]
  now?: Date
}

function formatCount(count: number, singular: string, plural = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`
}

function formatTemperatureRange(minimum: number, maximum: number): string {
  return `${minimum}°C to ${maximum}°C`
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
  }).format(parseLocalDate(value))
}

function formatTime(value: Date): string {
  return new Intl.DateTimeFormat('nb-NO', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function formatDeviationSeverity(severity: DeviationSeverity): string {
  return severity.toLowerCase().replace('_', ' ')
}

function isOpenDeviation(status: DeviationStatus): boolean {
  return status === 'OPEN' || status === 'IN_PROGRESS'
}

function getDeviationPriority(deviation: DeviationListItem): number {
  if (deviation.severity === 'CRITICAL') {
    return 0
  }

  if (deviation.severity === 'HIGH') {
    return 1
  }

  return 6
}

function getDeviationTone(deviation: DeviationListItem): WorkspaceAttentionTone {
  if (deviation.severity === 'CRITICAL' || deviation.severity === 'HIGH') {
    return 'critical'
  }

  if (deviation.status === 'OPEN') {
    return 'warning'
  }

  return 'primary'
}

function getTemperaturePriority(alertState: TemperatureAlertState): number {
  switch (alertState) {
    case 'OUT_OF_RANGE':
      return 2
    case 'OVERDUE':
      return 3
    case 'DUE_SOON':
      return 5
    default:
      return 10
  }
}

function getTemperatureTone(alertState: TemperatureAlertState): WorkspaceAttentionTone {
  if (alertState === 'OUT_OF_RANGE') {
    return 'critical'
  }

  if (alertState === 'OVERDUE' || alertState === 'DUE_SOON') {
    return 'warning'
  }

  return 'primary'
}

export function buildIKMatServiceSummary({
  checklistRuns,
  checklistNote = null,
  temperatureUnits,
  deviations,
  now = new Date(),
}: BuildIKMatServiceSummaryOptions): WorkspaceServiceSummary {
  const temperatureSummary = getTemperatureSummary(temperatureUnits, now)
  const openDeviationCount = deviations.filter((deviation) => isOpenDeviation(deviation.status)).length
  const activeChecklistRunCount =
    checklistRuns?.filter((run) => run.status !== 'COMPLETED' && run.status !== 'CANCELLED').length ?? null

  return {
    key: 'ik-mat',
    title: 'IK-mat',
    description: 'Daily kitchen routines, temperatures, and food safety follow-up.',
    to: {
      name: 'ik-mat-dashboard',
    },
    ctaLabel: 'Open dashboard',
    metrics: [
      {
        label: 'Active checklist runs',
        value: activeChecklistRunCount === null ? '—' : formatCount(activeChecklistRunCount, 'run'),
        tone: activeChecklistRunCount && activeChecklistRunCount > 0 ? 'primary' : 'neutral',
      },
      {
        label: 'Temperature units needing attention',
        value: formatCount(temperatureSummary.needsAttentionCount, 'unit'),
        tone: temperatureSummary.needsAttentionCount > 0 ? 'warning' : 'neutral',
      },
      {
        label: 'Open food deviations',
        value: formatCount(openDeviationCount, 'item'),
        tone: openDeviationCount > 0 ? 'critical' : 'neutral',
      },
    ],
    note: checklistNote,
  }
}

export function buildIKAlkoholServiceSummary({
  documents,
  deviations,
  note = null,
}: BuildIKAlkoholServiceSummaryOptions): WorkspaceServiceSummary {
  const documentList = getDocumentsWithStatus(documents, expiryWarningDays)
  const documentsNeedingAttention = documentList.filter((documentItem) => documentItem.status !== 'VALID').length
  const openDeviationCount = deviations.filter((deviation) => isOpenDeviation(deviation.status)).length
  const readyDocumentCount = documentList.filter((documentItem) => documentItem.status !== 'EXPIRED').length
  const readinessPercentage =
    documentList.length === 0 ? 0 : Math.round((readyDocumentCount / documentList.length) * 100)

  return {
    key: 'ik-alkohol',
    title: 'IK-alkohol',
    description: 'Alcohol-control documents, incidents, and service follow-up.',
    to: {
      name: 'ik-alkohol-dashboard',
    },
    ctaLabel: 'Open dashboard',
    metrics: [
      {
        label: 'Open alcohol deviations',
        value: formatCount(openDeviationCount, 'item'),
        tone: openDeviationCount > 0 ? 'critical' : 'neutral',
      },
      {
        label: 'Documents needing attention',
        value: formatCount(documentsNeedingAttention, 'document'),
        tone: documentsNeedingAttention > 0 ? 'warning' : 'neutral',
      },
      {
        label: 'Audit readiness',
        value: `${readinessPercentage}%`,
        tone: readinessPercentage < 100 ? 'primary' : 'neutral',
      },
    ],
    note,
  }
}

export function buildWorkspaceAttentionItems({
  checklistRuns,
  temperatureUnits,
  deviationsByService,
  documents,
  now = new Date(),
}: BuildWorkspaceAttentionItemsOptions): WorkspaceAttentionItem[] {
  const attentionItems: WorkspaceAttentionItem[] = []
  const temperatureUnitsWithStatus = getTemperatureUnitsWithStatus(temperatureUnits, now)
  const documentsWithStatus = getDocumentsWithStatus(documents, expiryWarningDays)

  checklistRuns
    .filter((run) => run.status === 'OVERDUE')
    .forEach((run) => {
      attentionItems.push({
        id: `checklist-${run.id}`,
        title: run.title,
        serviceLabel: 'IK-mat',
        reason: `Checklist overdue since ${formatDateTime(run.dueAt)}`,
        to: {
          name: 'ik-mat-dashboard',
        },
        tone: 'warning',
        priority: 3,
        sortAt: new Date(run.dueAt).getTime(),
      })
    })

  temperatureUnitsWithStatus
    .filter((unit) => ['OUT_OF_RANGE', 'OVERDUE', 'DUE_SOON'].includes(unit.alertState))
    .forEach((unit) => {
      const latestLog = unit.latestLog
      const reason =
        unit.alertState === 'OUT_OF_RANGE' && latestLog
          ? `Latest reading ${latestLog.temperatureCelsius.toFixed(1)}°C outside ${formatTemperatureRange(unit.minimumTemperature, unit.maximumTemperature)}`
          : unit.alertState === 'OVERDUE'
            ? `No reading logged by ${formatTime(unit.nextDueAt)}`
            : `Next reading due by ${formatTime(unit.nextDueAt)}`

      attentionItems.push({
        id: `temperature-${unit.id}`,
        title: unit.name,
        serviceLabel: 'IK-mat',
        reason,
        to: {
          name: 'ik-mat-temperature',
        },
        tone: getTemperatureTone(unit.alertState),
        priority: getTemperaturePriority(unit.alertState),
        sortAt: latestLog ? new Date(latestLog.measuredAt).getTime() : unit.nextDueAt.getTime(),
      })
    })

  ;(['IK_MAT', 'IK_ALKOHOL'] as const).forEach((serviceArea) => {
    deviationsByService[serviceArea]
      .filter((deviation) => isOpenDeviation(deviation.status))
      .forEach((deviation) => {
        attentionItems.push({
          id: `deviation-${deviation.id}`,
          title: deviation.title,
          serviceLabel: serviceArea === 'IK_MAT' ? 'IK-mat' : 'IK-alkohol',
          reason: `${deviation.category} • ${formatDeviationSeverity(deviation.severity)} deviation`,
          to: {
            name: serviceArea === 'IK_MAT' ? 'ik-mat-deviation' : 'ik-alkohol-deviation',
            query: {
              deviationId: deviation.id,
            },
          },
          tone: getDeviationTone(deviation),
          priority: getDeviationPriority(deviation),
          sortAt: new Date(deviation.reportedAt).getTime(),
        })
      })
  })

  documentsWithStatus
    .filter((documentItem) => documentItem.status === 'EXPIRED' || documentItem.status === 'EXPIRING')
    .forEach((documentItem) => {
      attentionItems.push({
        id: `document-${documentItem.id}`,
        title: documentItem.title,
        serviceLabel: 'IK-alkohol',
        reason:
          documentItem.status === 'EXPIRED'
            ? `Expired ${formatDate(documentItem.renewalDate)}`
            : `Expires ${formatDate(documentItem.renewalDate)}`,
        to: {
          name: 'ik-alkohol-documents',
        },
        tone: documentItem.status === 'EXPIRED' ? 'critical' : 'warning',
        priority: documentItem.status === 'EXPIRED' ? 4 : 5,
        sortAt: parseLocalDate(documentItem.renewalDate).getTime(),
      })
    })

  return attentionItems.sort((left, right) => {
    if (left.priority !== right.priority) {
      return left.priority - right.priority
    }

    return right.sortAt - left.sortAt
  })
}
