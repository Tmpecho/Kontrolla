<script setup lang="ts">
import type { ChecklistRun, ChecklistTaskExecution } from '@/checklists/model/checklist.types'
import {
  startChecklistRun,
  cancelChecklistRun,
  reopenChecklistRun,
  submitChecklistRun,
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

watch(
  () => props.run,
  (newRun) => {
    workingTasks.value = JSON.parse(JSON.stringify(newRun.tasks))
  },
  { immediate: true },
)

const isPending = computed(() => props.run.status === 'PENDING')
const isInProgress = computed(() => props.run.status === 'IN_PROGRESS')
const isCompleted = computed(() => props.run.status === 'COMPLETED')

const formattedDueDate = computed(() => {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(props.run.dueAt))
})

const statusMeta = computed(() => {
  const status = props.run.status
  const base = {
    label: status.replace(/_/g, ' '),
    class: 'status-default',
  }
  if (status === 'COMPLETED') return { ...base, class: 'status-success' }
  if (status === 'OVERDUE') return { ...base, class: 'status-critical' }
  if (status === 'IN_PROGRESS') return { ...base, class: 'status-primary' }
  return base
})

async function handleStart() {
  isSaving.value = true
  try {
    const updated = await startChecklistRun({
      organizationId: props.organizationId,
      establishmentId: props.establishmentId,
      checklistRunId: props.run.id,
    })
    emit('update:run', updated)
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

async function handleCancel() {
  isSaving.value = true
  try {
    const updated = await cancelChecklistRun({
      organizationId: props.organizationId,
      establishmentId: props.establishmentId,
      checklistRunId: props.run.id,
    })
    emit('update:run', updated)
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

async function handleReopen() {
  isSaving.value = true
  try {
    const updated = await reopenChecklistRun({
      organizationId: props.organizationId,
      establishmentId: props.establishmentId,
      checklistRunId: props.run.id,
    })
    emit('update:run', updated)
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

function toggleTask(taskId: string) {
  const task = workingTasks.value.find((t) => t.checklistTaskExecutionId === taskId)
  if (task) {
    task.executionStatus = task.executionStatus === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  }
}

async function handleSubmit() {
  isSaving.value = true
  try {
    const taskInputs: SubmitChecklistRunTaskInput[] = workingTasks.value.map((t) => ({
      checklistTaskExecutionId: t.checklistTaskExecutionId,
      executionStatus: t.executionStatus,
      comment: t.comment,
      verificationResult: t.verificationResult,
      measuredValue: t.measuredValue,
      enteredText: t.enteredText,
    }))

    const updated = await submitChecklistRun(
      {
        organizationId: props.organizationId,
        establishmentId: props.establishmentId,
        checklistRunId: props.run.id,
      },
      { tasks: taskInputs },
    )
    emit('update:run', updated)
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <article class="run-card">
    <header class="run-header">
      <div class="header-top">
        <h3 class="run-title">{{ run.title }}</h3>
        <span class="status-badge" :class="statusMeta.class">
          {{ statusMeta.label }}
        </span>
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
          :editable="isInProgress"
          @toggle="toggleTask"
        />
      </div>
      <p v-else class="empty-text">No tasks defined for this run.</p>
    </div>

    <!-- Action Footer -->
    <footer class="run-footer">
      <!-- PENDING STATE -->
      <template v-if="isPending">
        <button class="btn btn-primary" @click="handleStart" :disabled="isSaving">Start Run</button>
        <button class="btn btn-ghost" @click="handleCancel" :disabled="isSaving">Cancel</button>
      </template>

      <!-- IN PROGRESS STATE -->
      <template v-else-if="isInProgress">
        <button class="btn btn-primary" @click="handleSubmit" :disabled="isSaving">
          Submit Results
        </button>
        <button class="btn btn-danger" @click="handleCancel" :disabled="isSaving">
          Cancel Run
        </button>
      </template>

      <!-- COMPLETED STATE -->
      <template v-else-if="isCompleted">
        <button class="btn btn-secondary" @click="handleReopen" :disabled="isSaving">Reopen</button>
      </template>
    </footer>
  </article>
</template>

<style scoped>
/* Card Container */
.run-card {
  background-color: var(--color-container);
  border-radius: 0.75rem;
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-muted);
}

/* Header */
.run-header {
  padding: 1.5rem 1.5rem 1rem;
  border-bottom: 1px solid var(--color-border-muted);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.5rem;
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

/* Status Badges */
.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
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

/* Meta Grid */
.run-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  padding: 1rem 1.5rem;
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border-muted);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.meta-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
}

.meta-value {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

/* Tasks Section */
.tasks-container {
  padding: 1rem 1.5rem 1.5rem;
}

.tasks-heading {
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  margin: 0 0 0.5rem;
}

.tasks-list {
  border-top: 1px solid var(--color-border-muted);
  margin-top: 0.5rem;
}

.empty-text {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  text-align: center;
  padding: 1rem;
}

/* Footer & Buttons */
.run-footer {
  padding: 1rem 1.5rem;
  background-color: var(--color-white);
  border-top: 1px solid var(--color-border-muted);
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.btn {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 0.875rem;
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: var(--color-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #004a94;
}

.btn-secondary {
  background-color: white;
  color: var(--color-text-primary);
  border-color: var(--color-border-muted);
}

.btn-danger {
  background-color: white;
  color: var(--color-critical);
  border-color: var(--color-critical);
}

.btn-danger:hover:not(:disabled) {
  background-color: #fef2f2;
}

.btn-ghost {
  background-color: transparent;
  color: var(--color-text-secondary);
}

.btn-ghost:hover:not(:disabled) {
  background-color: #f1f5f9;
}
</style>
