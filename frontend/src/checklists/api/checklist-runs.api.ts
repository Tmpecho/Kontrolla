import { requestJson } from '@/shared/api/http'
import type {
  ChecklistRun,
  ChecklistServiceArea,
  ChecklistRunStatus,
  PageResponse,
  ChecklistTaskExecutionStatus,
  ChecklistVerificationResult,
} from '@/checklists/model/checklist.types'

// --- Types ---

export type ListChecklistRunsParams = {
  organizationId: string
  establishmentId: string
  serviceArea: ChecklistServiceArea
  statuses?: ChecklistRunStatus[]
  assignedUserId?: string
  assignedToMe?: boolean
  dueFrom?: string
  dueTo?: string
  page?: number
  size?: number
}

export type AssignChecklistRunInput = {
  assignedUserIds: string[]
}

export type SubmitChecklistRunTaskInput = {
  checklistTaskExecutionId: string
  executionStatus: ChecklistTaskExecutionStatus
  comment?: string | null
  verificationResult?: ChecklistVerificationResult | null
  measuredValue?: number | null
  enteredText?: string | null
}

export type SubmitChecklistRunInput = {
  tasks: SubmitChecklistRunTaskInput[]
}

type ContextParams = {
  organizationId: string
  establishmentId: string
}

// --- Internal Helper ---

const getBaseUrl = (params: ContextParams) =>
  `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/checklists/runs`

// --- API Functions ---

export async function listChecklistRuns(
  params: ListChecklistRunsParams,
): Promise<PageResponse<ChecklistRun>> {
  return requestJson<PageResponse<ChecklistRun>>(getBaseUrl(params), {
    query: {
      serviceArea: params.serviceArea,
      statuses: params.statuses,
      assignedUserId: params.assignedUserId,
      assignedToMe: params.assignedToMe,
      dueFrom: params.dueFrom,
      dueTo: params.dueTo,
      page: params.page,
      size: params.size,
    },
  })
}

export async function getChecklistRun(
  params: ContextParams & { checklistRunId: string },
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}`)
}

export async function assignChecklistRun(
  params: ContextParams & { checklistRunId: string },
  input: AssignChecklistRunInput,
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/assignments`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function removeChecklistRunAssignment(
  params: ContextParams & { checklistRunId: string; assignmentId: string },
): Promise<void> {
  return requestJson<void>(
    `${getBaseUrl(params)}/${params.checklistRunId}/assignments/${params.assignmentId}`,
    {
      method: 'DELETE',
    },
  )
}

export async function startChecklistRun(
  params: ContextParams & { checklistRunId: string },
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/start`, {
    method: 'POST',
  })
}

export async function submitChecklistRun(
  params: ContextParams & { checklistRunId: string },
  input: SubmitChecklistRunInput,
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/submit`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function reopenChecklistRun(
  params: ContextParams & { checklistRunId: string },
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/reopen`, {
    method: 'POST',
  })
}

export async function cancelChecklistRun(
  params: ContextParams & { checklistRunId: string },
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/cancel`, {
    method: 'POST',
  })
}

export async function resetChecklistRun(
  params: ContextParams & { checklistRunId: string },
): Promise<ChecklistRun> {
  return requestJson<ChecklistRun>(`${getBaseUrl(params)}/${params.checklistRunId}/reset`, {
    method: 'POST',
  })
}
