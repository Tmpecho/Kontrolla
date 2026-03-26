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

export type ChecklistRunItem = {
  checklistRunItemId: string
  sourceChecklistItemDefinitionId: string | null
  prompt: string
  instructionText: string | null
  responseType: 'BOOLEAN' | 'TEXT' | 'NUMBER'
  required: boolean
  sortOrder: number
  answerBoolean: boolean | null
  answerText: string | null
  answerNumber: number | null
  answerNote: string | null
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
  items: ChecklistRunItem[]
  events: ChecklistRunEvent[]
}
