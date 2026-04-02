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

export function selectLatestChecklistRuns(runs: ChecklistRun[]): ChecklistRun[] {
  const latestRunsByDefinitionGroup = new Map<string, ChecklistRun>()

  for (const run of runs) {
    const current = latestRunsByDefinitionGroup.get(run.definitionGroupId)

    if (!current || compareRunsByRecency(run, current) < 0) {
      latestRunsByDefinitionGroup.set(run.definitionGroupId, run)
    }
  }

  return [...latestRunsByDefinitionGroup.values()].sort(compareRunsByRecency)
}
