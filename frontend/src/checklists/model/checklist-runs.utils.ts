import type { ChecklistRun } from '@/checklists/model/checklist.types'

function toTimestamp(value: string | null): number {
  return value ? new Date(value).getTime() : Number.NEGATIVE_INFINITY
}

function compareRunsByRecency(left: ChecklistRun, right: ChecklistRun): number {
  return (
    toTimestamp(right.dueAt) - toTimestamp(left.dueAt) ||
    toTimestamp(right.updatedAt) - toTimestamp(left.updatedAt) ||
    toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
  )
}

export function sortChecklistRunsByRecency(runs: ChecklistRun[]): ChecklistRun[] {
  return [...runs].sort(compareRunsByRecency)
}
