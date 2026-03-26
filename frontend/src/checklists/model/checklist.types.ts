export type ChecklistServiceArea = 'IK_MAT' | 'IK_ALKOHOL'

export type ChecklistRunStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED'

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ChecklistRunAssignment = {
  id: string
  assignedUserId: string
  assignedByUserId: string
  assignedAt: string
}

export type ChecklistTaskKind = 'ACTION' | 'VERIFICATION' | 'MEASUREMENT' | 'TEXT_ENTRY'

export type ChecklistTaskExecutionStatus = 'PENDING' | 'COMPLETED' | 'SKIPPED'

export type ChecklistVerificationResult = 'VERIFIED' | 'NOT_VERIFIED'

export type ChecklistTaskExecution = {
  checklistTaskExecutionId: string
  sourceChecklistTaskDefinitionId: string | null
  title: string
  details: string | null
  taskKind: ChecklistTaskKind
  required: boolean
  sortOrder: number
  measurementUnit: string | null
  minimumAllowedValue: number | null
  maximumAllowedValue: number | null
  executionStatus: ChecklistTaskExecutionStatus
  resolvedAt: string | null
  resolvedByUserId: string | null
  comment: string | null
  verificationResult: ChecklistVerificationResult | null
  measuredValue: number | null
  enteredText: string | null
}

export type ChecklistRunEvent = {
  id: string
  eventType: 'CREATED' | 'ASSIGNED' | 'STARTED' | 'COMPLETED' | 'REOPENED' | 'CANCELLED'
  actorUserId: string | null
  occurredAt: string
  metadataJson: string | null
}

export type ChecklistRun = {
  id: string
  checklistDefinitionId: string
  definitionGroupId: string
  establishmentId: string
  serviceArea: ChecklistServiceArea
  title: string
  description: string | null
  dueAt: string
  status: ChecklistRunStatus
  startedAt: string | null
  completedAt: string | null
  completedByUserId: string | null
  createdByUserId: string
  createdAt: string
  updatedAt: string
  assignments: ChecklistRunAssignment[]
  tasks: ChecklistTaskExecution[]
  events: ChecklistRunEvent[]
}
