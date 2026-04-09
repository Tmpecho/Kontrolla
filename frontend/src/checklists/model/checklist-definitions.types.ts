import type { ChecklistServiceArea, PageResponse } from '@/checklists/model/checklist.types'

export type ChecklistDefinitionStatus = 'DRAFT' | 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED'

export type ChecklistScheduleType = 'ONE_OFF' | 'DAILY' | 'WEEKLY' | 'MONTHLY'

export type ChecklistTaskKind = 'ACTION' | 'VERIFICATION' | 'MEASUREMENT' | 'TEXT_ENTRY'

export type ChecklistDefinitionTask = {
  id: string
  title: string
  details: string | null
  taskKind: ChecklistTaskKind
  required: boolean
  sortOrder: number
  measurementUnit: string | null
  minimumAllowedValue: number | null
  maximumAllowedValue: number | null
}

export type ChecklistDefinitionSchedule = {
  id: string
  scheduleType: ChecklistScheduleType
  startDate: string
  endDate: string | null
  dueTime: string | null
  weekdayMask: number | null
  dayOfMonth: number | null
  timezone: string
  active: boolean
  createdByUserId: string
  updatedByUserId: string
}

export type ChecklistDefinition = {
  id: string
  definitionGroupId: string
  versionNumber: number
  establishmentId: string
  serviceArea: ChecklistServiceArea
  title: string
  description: string | null
  status: ChecklistDefinitionStatus
  effectiveFrom: string
  effectiveTo: string | null
  createdByUserId: string
  updatedByUserId: string
  createdAt: string
  updatedAt: string
  tasks: ChecklistDefinitionTask[]
  schedules: ChecklistDefinitionSchedule[]
}

export type ChecklistDefinitionPage = PageResponse<ChecklistDefinition>
