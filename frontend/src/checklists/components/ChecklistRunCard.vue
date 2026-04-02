<script setup lang="ts">
import type { ChecklistRun, ChecklistTaskExecution } from '@/checklists/model/checklist.types'
import {
  cancelChecklistRun,
  reopenChecklistRun,
  resetChecklistRun,
  updateChecklistRunTask,
  type SubmitChecklistRunTaskInput,
} from '@/checklists/api/checklist-runs.api'
import ChecklistTaskItem from './ChecklistTaskItem.vue'
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  run: ChecklistRun
  organizationId: string
  establishmentId: string
}>()

const emit = defineEmits<{
  (e: 'update:run', run: ChecklistRun): void
}>()

const workingTasks = ref<ChecklistTaskExecution[]>([])
const isSaving = ref(false)

const cloneTasks = (tasks?: ChecklistTaskExecution[]) =>
  tasks ? JSON.parse(JSON.stringify(tasks)) : []

watch(
  () => props.run.tasks,
  (tasks) => {
    workingTasks.value = cloneTasks(tasks)
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

const baseParams = computed(() => ({
  organizationId: props.organizationId,
  establishmentId: props.establishmentId,
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

const toSubmitInput = (task: ChecklistTaskExecution): SubmitChecklistRunTaskInput => ({
  checklistTaskExecutionId: task.checklistTaskExecutionId,
  executionStatus: task.executionStatus,
  comment: task.comment,
  verificationResult: task.verificationResult,
  measuredValue: task.measuredValue,
  enteredText: task.enteredText,
})

const handleTaskUpdate = async (updatedTask: ChecklistTaskExecution) => {
  workingTasks.value = workingTasks.value.map((task) =>
    task.checklistTaskExecutionId === updatedTask.checklistTaskExecutionId ? updatedTask : task,
  )

  try {
    const updatedRun = await updateChecklistRunTask(
      { ...baseParams.value, taskId: updatedTask.checklistTaskExecutionId },
      toSubmitInput(updatedTask),
    )
    emit('update:run', updatedRun)
  } catch (e) {
    console.error('Failed to save task update', e)
    workingTasks.value = cloneTasks(props.run.tasks)
  }
}
</script>

<template>
  <article class="run-card">
    <header class="run-header">
      <div class="header-top">
        <h3 class="run-title">{{ run.title }}</h3>
        <span class="status-badge" :class="statusMeta.class">{{ statusMeta.label }}</span>
      </div>
      <p v-if="run.description" class="run-description">{{ run.description }}</p>
    </header>

    <div class="run-meta-grid">
      <div class="meta-item">
        <span class="meta-label">Due</span>
        <span class="meta-value">{{ formattedDueDate }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">Assignees</span>
        <span class="meta-value">{{ run.assignments.length || 'Unassigned' }}</span>
      </div>
    </div>

    <!-- Tasks Section -->
    <div class="tasks-container">
      <h4 class="tasks-heading">Tasks ({{ run.tasks.length }})</h4>

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

    <footer class="run-footer">
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
          <span class="auto-save-text">Changes save automatically</span>
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
  border-radius: 1cqh;
  box-shadow: var(--shadow-elevated);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.run-header,
.tasks-container,
.run-footer {
  padding: 1rem 1.5rem;
}
.run-header {
  padding-top: 1.5rem;
  border-bottom: 1px solid var(--color-border-muted);
}
.run-footer {
  background: var(--color-white);
  border-top: 1px solid var(--color-border-muted);
  flex-wrap: wrap;
}
.tasks-container {
  padding-bottom: 1.5rem;
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
  margin-bottom: 0.5rem;
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
.run-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-muted);
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.run-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  line-height: 1.3;
}
.run-description {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.meta-value {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
}
.empty-text {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  text-align: center;
  padding: 1rem;
}

.status-badge,
.meta-label,
.tasks-heading {
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.75rem;
}
.meta-label,
.tasks-heading {
  color: var(--color-text-secondary);
}
.tasks-heading {
  font-size: 0.8125rem;
  margin: 0 0 0.5rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 99px;
  white-space: nowrap;
}
.status-default {
  background: var(--color-surface);
  color: var(--color-text-secondary);
}
.status-primary {
  background: #eff6ff;
  color: var(--color-primary);
}
.status-success {
  background: #ecfdf5;
  color: var(--color-success);
}
.status-critical {
  background: #fef2f2;
  color: var(--color-critical);
}

.tasks-list {
  border-top: 1px solid var(--color-border-muted);
  margin-top: 0.5rem;
}

.btn {
  font: 600 0.875rem var(--font-sans, inherit);
  padding: 0.625rem 1.25rem;
  border-radius: 1cqh;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.15s;
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
