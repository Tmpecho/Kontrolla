<script setup lang="ts">
import type { ChecklistRun, ChecklistTaskExecution } from '@/checklists/model/checklist.types'
import {
  cancelChecklistRun,
  reopenChecklistRun,
  resetChecklistRun,
  updateChecklistRunTask,
  type SubmitChecklistRunTaskInput,
} from '@/checklists/api/checklist-runs.api'
import { AlertCircle, Check, Circle, CircleDashed } from 'lucide-vue-next'
import ChecklistTaskItem from './ChecklistTaskItem.vue'
import { computed, ref, watch } from 'vue'

type AutoSaveState = 'idle' | 'saving' | 'saved' | 'failed'

const props = defineProps<{
  run: ChecklistRun
  organizationId: string
  establishmentId: string
  selected?: boolean
  forceExpanded?: boolean
  showSetupActions?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:run', run: ChecklistRun): void
  (e: 'edit:definitionGroup', definitionGroupId: string): void
}>()

const workingTasks = ref<ChecklistTaskExecution[]>([])
const isSaving = ref(false)
const isExpanded = ref(false)
const autoSaveState = ref<AutoSaveState>('idle')

const cloneTasks = (tasks?: ChecklistTaskExecution[]) =>
  tasks ? JSON.parse(JSON.stringify(tasks)) : []

watch(
  () => props.run.tasks,
  (tasks) => {
    workingTasks.value = cloneTasks(tasks)
  },
  { immediate: true },
)

watch(
  () => props.forceExpanded,
  (forceExpanded) => {
    if (forceExpanded) {
      isExpanded.value = true
    }
  },
  { immediate: true },
)

const isPending = computed(() => props.run.status === 'PENDING')
const isInProgress = computed(() => props.run.status === 'IN_PROGRESS')
const isCompleted = computed(() => props.run.status === 'COMPLETED')
const isCancelled = computed(() => props.run.status === 'CANCELLED')
const isOverdue = computed(() => props.run.status === 'OVERDUE')
const isEditable = computed(() => isPending.value || isInProgress.value || isOverdue.value)
const formattedDueDate = computed(() =>
  new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(props.run.dueAt)),
)
const assignedSummary = computed(() => {
  const count = props.run.assignments.length
  const assignedNames = props.run.assignments
    .map((assignment) => assignment.assignedUserName?.trim())
    .filter((name): name is string => Boolean(name))

  if (count === 0) {
    return 'Unassigned'
  }

  if (assignedNames.length === count && count <= 2) {
    return assignedNames.join(', ')
  }

  return count === 1 ? '1 assigned' : `${count} assigned`
})

const STATUS_CLASSES: Record<string, string> = {
  COMPLETED: 'status-success',
  OVERDUE: 'status-critical',
  IN_PROGRESS: 'status-primary',
  CANCELLED: 'status-default',
}

const statusMeta = computed(() => ({
  label: props.run.status.replace(/_/g, ' '),
  class: STATUS_CLASSES[props.run.status] ?? 'status-default',
}))

const completedTaskCount = computed(
  () => workingTasks.value.filter((task) => task.executionStatus === 'COMPLETED').length,
)

const totalTaskCount = computed(() => workingTasks.value.length)

const remainingRequiredTaskCount = computed(
  () =>
    workingTasks.value.filter((task) => task.required && task.executionStatus !== 'COMPLETED')
      .length,
)

const issueTaskCount = computed(
  () =>
    workingTasks.value.filter((task) => {
      if (task.taskKind === 'VERIFICATION') {
        return task.verificationResult === 'NOT_VERIFIED'
      }

      if (task.taskKind !== 'MEASUREMENT' || task.measuredValue === null) {
        return false
      }

      if (task.minimumAllowedValue !== null && task.measuredValue < task.minimumAllowedValue) {
        return true
      }

      if (task.maximumAllowedValue !== null && task.measuredValue > task.maximumAllowedValue) {
        return true
      }

      return false
    }).length,
)

const progressStates = computed(() => [
  {
    key: 'completed',
    label: `${completedTaskCount.value}/${totalTaskCount.value} completed`,
    state:
      totalTaskCount.value > 0 && completedTaskCount.value === totalTaskCount.value
        ? 'complete'
        : completedTaskCount.value > 0
          ? 'current'
          : 'upcoming',
  },
  {
    key: 'required-left',
    label: `${remainingRequiredTaskCount.value} required left`,
    state: remainingRequiredTaskCount.value === 0 ? 'complete' : 'upcoming',
  },
  {
    key: 'issues',
    label: `${issueTaskCount.value} issues`,
    state: issueTaskCount.value > 0 ? 'issue' : 'complete',
  },
])

const baseParams = computed(() => ({
  organizationId: props.organizationId,
  establishmentId: props.run.establishmentId || props.establishmentId,
  checklistRunId: props.run.id,
}))

const withLoading = (apiAction: () => Promise<ChecklistRun>) => async () => {
  isSaving.value = true
  try {
    emit('update:run', await apiAction())
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

const handleResetRun = withLoading(() => resetChecklistRun(baseParams.value))
const handleCancel = withLoading(() => cancelChecklistRun(baseParams.value))
const handleReopen = withLoading(() => reopenChecklistRun(baseParams.value))
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}
const handleEditSetup = () => {
  emit('edit:definitionGroup', props.run.definitionGroupId)
}

const toSubmitInput = (task: ChecklistTaskExecution): SubmitChecklistRunTaskInput => ({
  checklistTaskExecutionId: task.checklistTaskExecutionId,
  executionStatus: task.executionStatus,
  comment: task.comment,
  verificationResult: task.verificationResult,
  measuredValue: task.measuredValue,
  enteredText: task.enteredText,
})

const autoSaveMeta = computed(() => {
  switch (autoSaveState.value) {
    case 'saving':
      return { label: 'Saving changes...', className: 'auto-save-saving' }
    case 'saved':
      return { label: 'All changes saved', className: 'auto-save-saved' }
    case 'failed':
      return { label: 'Failed to save changes', className: 'auto-save-failed' }
    default:
      return { label: 'Changes save automatically', className: '' }
  }
})

const handleTaskUpdate = async (updatedTask: ChecklistTaskExecution) => {
  workingTasks.value = workingTasks.value.map((task) =>
    task.checklistTaskExecutionId === updatedTask.checklistTaskExecutionId ? updatedTask : task,
  )
  autoSaveState.value = 'saving'

  try {
    const updatedRun = await updateChecklistRunTask(
      { ...baseParams.value, taskId: updatedTask.checklistTaskExecutionId },
      toSubmitInput(updatedTask),
    )
    autoSaveState.value = 'saved'
    emit('update:run', updatedRun)
  } catch (e) {
    console.error('Failed to save task update', e)
    workingTasks.value = cloneTasks(props.run.tasks)
    autoSaveState.value = 'failed'
  }
}
</script>

<template>
  <article class="run-card" :class="{ 'run-card-selected': selected }">
    <header
      class="run-header"
      role="button"
      tabindex="0"
      :aria-expanded="isExpanded"
      :aria-label="isExpanded ? 'Collapse checklist run' : 'Expand checklist run'"
      @click="toggleExpanded"
      @keydown.enter.prevent="toggleExpanded"
      @keydown.space.prevent="toggleExpanded"
    >
      <div class="header-top">
        <div class="header-copy">
          <h3 class="run-title">{{ run.title }}</h3>
        </div>
        <div class="header-actions">
          <button
            v-if="showSetupActions"
            type="button"
            class="btn btn-secondary btn-compact"
            @click.stop="handleEditSetup"
          >
            Edit setup
          </button>
          <span class="status-badge" :class="statusMeta.class">{{ statusMeta.label }}</span>
          <span class="header-divider" aria-hidden="true"></span>
          <svg
            class="toggle-arrow"
            :class="{ 'toggle-arrow-expanded': isExpanded }"
            viewBox="0 0 20 20"
            aria-hidden="true"
          >
            <path d="M5 7.5L10 12.5L15 7.5" />
          </svg>
        </div>
      </div>
      <p v-if="run.description" class="run-description">{{ run.description }}</p>
      <div class="header-meta">
        <div class="header-meta-item">
          <span class="header-meta-label">Due</span>
          <span class="header-meta-value">{{ formattedDueDate }}</span>
        </div>
        <div class="header-meta-item">
          <span class="header-meta-label">Assigned</span>
          <span class="header-meta-value">{{ assignedSummary }}</span>
        </div>
      </div>
      <div class="progress-strip" aria-label="Checklist progress summary">
        <div class="progress-track" aria-hidden="true"></div>
        <div class="progress-steps">
          <div
            v-for="step in progressStates"
            :key="step.key"
            class="progress-step"
            :class="`progress-step-${step.state}`"
          >
            <component
              :is="
                step.state === 'issue'
                  ? AlertCircle
                  : step.state === 'complete'
                    ? Check
                    : step.state === 'current'
                      ? Circle
                      : CircleDashed
              "
              class="progress-step-icon"
              aria-hidden="true"
            />
            <div class="progress-step-copy">
              <span class="progress-step-label">{{ step.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Tasks Section -->
    <div v-if="isExpanded" class="tasks-container">
      <div class="tasks-header">
        <h4 class="tasks-heading">Tasks ({{ run.tasks.length }})</h4>
      </div>

      <div v-if="workingTasks.length > 0" class="tasks-list">
        <ChecklistTaskItem
          v-for="task in workingTasks"
          :key="task.checklistTaskExecutionId"
          :task="task"
          :editable="isEditable"
          @update:task="handleTaskUpdate"
        />
      </div>
      <p v-else class="empty-text">No tasks defined for this run.</p>
    </div>

    <footer v-if="isExpanded" class="run-footer">
      <template v-if="isEditable">
        <div class="footer-left">
          <button
            v-if="isInProgress"
            class="btn btn-danger-ghost"
            @click="handleResetRun"
            :disabled="isSaving"
          >
            Reset to Pending
          </button>
          <button class="btn btn-danger-ghost" @click="handleCancel" :disabled="isSaving">
            Cancel Run
          </button>
        </div>
        <div class="footer-right">
          <span class="auto-save-text" :class="autoSaveMeta.className">{{
            autoSaveMeta.label
          }}</span>
        </div>
      </template>

      <template v-else-if="isCompleted || isCancelled">
        <div class="footer-right" style="margin-left: auto">
          <button class="btn btn-secondary" @click="handleReopen" :disabled="isSaving">
            Reopen
          </button>
        </div>
      </template>
    </footer>
  </article>
</template>

<style scoped>
.run-card {
  background: var(--color-container);
  border: 1px solid var(--color-border-muted);
  border-radius: 0.5cqh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.run-card-selected {
  border-color: var(--color-primary);
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.run-header,
.tasks-container,
.run-footer {
  padding: 0.75rem 0.95rem;
}
.run-header {
  padding-top: 0.875rem;
  padding-bottom: 0.875rem;
  border-bottom: 1px solid var(--color-border-muted);
  cursor: pointer;
}
.run-footer {
  background: color-mix(in srgb, var(--color-surface) 45%, white);
  border-top: 1px solid var(--color-border-muted);
  flex-wrap: wrap;
}
.tasks-container {
  padding-bottom: 0.875rem;
}

.header-top,
.run-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.header-top {
  align-items: flex-start;
  margin-bottom: 0.3rem;
}
.header-copy {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}
.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.header-divider {
  width: 1px;
  align-self: stretch;
  background: var(--color-border-muted);
  opacity: 0.9;
}
.footer-left,
.footer-right {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}
.footer-right {
  margin-left: auto;
}
.auto-save-text {
  color: var(--color-text-secondary);
}
.auto-save-text.auto-save-saving {
  color: var(--color-primary);
}
.auto-save-text.auto-save-saved {
  color: var(--color-success);
}
.auto-save-text.auto-save-failed {
  color: var(--color-critical);
}
.run-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
  line-height: 1.2;
  text-align: left;
}
.run-description {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-text-secondary);
  line-height: 1.28;
}
.header-meta {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.45rem;
}
.header-meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.08rem;
  min-width: 0;
}
.header-meta-label {
  font-size: 0.625rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}
.header-meta-value {
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-text-primary);
}
.progress-strip {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-top: 0.55rem;
  position: relative;
}
.progress-track {
  width: 100%;
  border-top: 2px solid color-mix(in srgb, var(--color-primary) 75%, white);
}
.progress-steps {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}
.progress-step {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}
.progress-step-icon {
  width: 1rem;
  height: 1rem;
  color: #c6c6c6;
  flex-shrink: 0;
}
.progress-step-copy {
  display: flex;
  min-width: 0;
}
.progress-step-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--color-text-primary);
}
.progress-step-complete .progress-step-icon {
  color: #24a148;
}
.progress-step-current .progress-step-icon {
  color: var(--color-primary);
}
.progress-step-upcoming .progress-step-icon {
  color: var(--color-text-secondary);
}
.progress-step-issue .progress-step-icon {
  color: var(--color-critical);
}
.progress-step-issue .progress-step-label {
  color: var(--color-critical);
}

@media (max-width: 720px) {
  .progress-steps {
    gap: 0.75rem;
  }

  .progress-step {
    min-width: min(100%, 12rem);
  }
}
.toggle-arrow {
  width: 1.125rem;
  height: 1.125rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.85;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 160ms ease;
  color: var(--color-primary);
  flex-shrink: 0;
}
.toggle-arrow-expanded {
  transform: rotate(180deg);
}
.meta-value {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-primary);
}
.empty-text {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  text-align: center;
  padding: 0.5rem;
}

.status-badge,
.tasks-heading {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.625rem;
}
.tasks-heading {
  color: var(--color-text-secondary);
}
.tasks-heading {
  font-size: 0.6875rem;
  margin: 0 0 0.25rem;
}
.tasks-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(10.5rem, 12.5rem);
  align-items: end;
  gap: 0.5rem;
}

.status-badge {
  padding: 0.18rem 0.45rem;
  border-radius: 0.5cqh;
  white-space: nowrap;
  border: 1px solid transparent;
}
.status-default {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border-color: var(--color-border-muted);
}
.status-primary {
  background: #eff6ff;
  color: var(--color-primary);
  border-color: #b6d3ff;
}
.status-success {
  background: #ecfdf5;
  color: var(--color-success);
  border-color: #b8e2ca;
}
.status-critical {
  background: #fef2f2;
  color: var(--color-critical);
  border-color: #f0b8b8;
}

.tasks-list {
  border-top: 1px solid var(--color-border-muted);
  margin-top: 0.2rem;
}

@media (max-width: 720px) {
  .tasks-header {
    grid-template-columns: minmax(0, 1fr);
  }

  .tasks-result-heading {
    display: none;
  }
}

.btn {
  font: 600 0.75rem var(--font-sans, inherit);
  padding: 0.42rem 0.7rem;
  border-radius: 0.5cqh;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.15s;
}
.btn-compact {
  padding-inline: 0.6rem;
}
.btn:disabled {
  opacity: 0.6;
  pointer-events: none;
}
.btn-primary {
  background: var(--color-primary);
  color: #fff;
}
.btn-primary:hover {
  background: #004a94;
}
.btn-secondary {
  background: #fff;
  color: var(--color-text-primary);
  border-color: var(--color-border-muted);
}
.btn-secondary:hover {
  background: var(--color-surface);
}
.btn-danger-ghost {
  background: transparent;
  color: var(--color-critical);
}
.btn-danger-ghost:hover {
  background: #fef2f2;
}

.auto-save-text {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  font-style: italic;
}
</style>
