import { requestJson } from '@/shared/api/http'
import type { ChecklistServiceArea } from '@/checklists/model/checklist.types'
import type {
  ChecklistDefinition,
  ChecklistDefinitionPage,
  ChecklistDefinitionStatus,
  ChecklistScheduleType,
  ChecklistTaskKind,
} from '@/checklists/model/checklist-definitions.types'

type ContextParams = {
  organizationId: string
  establishmentId: string
}

export type ChecklistDefinitionTaskInput = {
  title: string
  details?: string | null
  taskKind: ChecklistTaskKind
  required: boolean
  sortOrder: number
  measurementUnit?: string | null
  minimumAllowedValue?: number | null
  maximumAllowedValue?: number | null
}

export type ChecklistDefinitionScheduleInput = {
  scheduleType: ChecklistScheduleType
  startDate: string
  endDate?: string | null
  dueTime?: string | null
  weekdayMask?: number | null
  dayOfMonth?: number | null
  timezone?: string | null
  active?: boolean | null
}

export type UpsertChecklistDefinitionInput = {
  title: string
  description?: string | null
  serviceArea: ChecklistServiceArea
  status?: ChecklistDefinitionStatus
  tasks: ChecklistDefinitionTaskInput[]
  schedules: ChecklistDefinitionScheduleInput[]
}

function getBaseUrl(params: ContextParams): string {
  return `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/checklists/definitions`
}

export async function listChecklistDefinitions(
  params: ContextParams & { serviceArea: ChecklistServiceArea; page?: number; size?: number },
): Promise<ChecklistDefinitionPage> {
  return requestJson<ChecklistDefinitionPage>(getBaseUrl(params), {
    query: {
      serviceArea: params.serviceArea,
      page: params.page,
      size: params.size,
    },
  })
}

export async function createChecklistDefinition(
  params: ContextParams,
  input: UpsertChecklistDefinitionInput,
): Promise<ChecklistDefinition> {
  return requestJson<ChecklistDefinition>(getBaseUrl(params), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export async function updateChecklistDefinition(
  params: ContextParams & { checklistDefinitionId: string },
  input: UpsertChecklistDefinitionInput,
): Promise<ChecklistDefinition> {
  return requestJson<ChecklistDefinition>(`${getBaseUrl(params)}/${params.checklistDefinitionId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}
