<script setup lang="ts">
import type {
  ChecklistTaskExecution,
  ChecklistVerificationResult,
} from '@/checklists/model/checklist.types'
import { computed } from 'vue'

const props = defineProps<{
  task: ChecklistTaskExecution
  editable?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:task', task: ChecklistTaskExecution): void
}>()

const statusColor = computed(() => {
  if (props.editable && props.task.executionStatus === 'PENDING') {
    return 'var(--color-border-muted)'
  }
  switch (props.task.executionStatus) {
    case 'COMPLETED':
      return 'var(--color-success)'
    case 'SKIPPED':
      return 'var(--color-warning)'
    default:
      return 'var(--color-text-secondary)'
  }
})

const statusLabel = computed(() => {
  if (props.task.executionStatus === 'PENDING') return 'Pending'
  return props.task.executionStatus.charAt(0) + props.task.executionStatus.slice(1).toLowerCase()
})

function updateTask(fields: Partial<ChecklistTaskExecution>) {
  emit('update:task', { ...props.task, ...fields })
}

function toggleAction() {
  const newStatus = props.task.executionStatus === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  updateTask({
    executionStatus: newStatus,
    resolvedAt: newStatus === 'COMPLETED' ? new Date().toISOString() : null,
  })
}

function handleVerificationChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value as ChecklistVerificationResult
  if (value) {
    updateTask({
      verificationResult: value,
      executionStatus: 'COMPLETED',
      resolvedAt: new Date().toISOString(),
    })
  } else {
    updateTask({ verificationResult: null, executionStatus: 'PENDING', resolvedAt: null })
  }
}

function handleMeasurementChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (value === '') {
    updateTask({ measuredValue: null, executionStatus: 'PENDING', resolvedAt: null })
  } else {
    const num = parseFloat(value)
    updateTask({
      measuredValue: num,
      executionStatus: 'COMPLETED',
      resolvedAt: new Date().toISOString(),
    })
  }
}

function handleTextChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (value.trim() === '') {
    updateTask({ enteredText: null, executionStatus: 'PENDING', resolvedAt: null })
  } else {
    updateTask({
      enteredText: value,
      executionStatus: 'COMPLETED',
      resolvedAt: new Date().toISOString(),
    })
  }
}
</script>

<template>
  <div class="task-item" :class="{ 'is-completed': task.executionStatus === 'COMPLETED' }">
    <div class="task-indicator" :style="{ backgroundColor: statusColor }" />

    <div class="task-content">
      <div class="task-header">
        <span class="task-title">{{ task.title }}</span>
        <span class="task-badge">{{ task.taskKind }}</span>
      </div>
      <span v-if="task.details" class="task-details">{{ task.details }}</span>

      <div v-if="editable" class="task-input-area">
        <!-- TYPE: ACTION (Checkbox) -->
        <template v-if="task.taskKind === 'ACTION'">
          <label class="checkbox-label">
            <input
              type="checkbox"
              :checked="task.executionStatus === 'COMPLETED'"
              @change="toggleAction"
            />
            <span>Mark as Done</span>
          </label>
        </template>

        <template v-else-if="task.taskKind === 'VERIFICATION'">
          <select
            :value="task.verificationResult || ''"
            @change="handleVerificationChange"
            class="task-select"
          >
            <option value="">Select Result...</option>
            <option value="VERIFIED">Verified (OK)</option>
            <option value="NOT_VERIFIED">Not Verified (Issue)</option>
          </select>
        </template>

        <template v-else-if="task.taskKind === 'MEASUREMENT'">
          <div class="input-group">
            <input
              type="number"
              :value="task.measuredValue || ''"
              @change="handleMeasurementChange"
              class="task-input"
              placeholder="Enter value"
              step="0.1"
            />
            <span v-if="task.measurementUnit" class="input-unit">{{ task.measurementUnit }}</span>
          </div>
          <div
            v-if="task.minimumAllowedValue !== null || task.maximumAllowedValue !== null"
            class="input-hint"
          >
            Range: {{ task.minimumAllowedValue ?? '-∞' }} to {{ task.maximumAllowedValue ?? '∞' }}
          </div>
        </template>

        <template v-else-if="task.taskKind === 'TEXT_ENTRY'">
          <input
            type="text"
            :value="task.enteredText || ''"
            @input="handleTextChange"
            class="task-input"
            placeholder="Enter notes..."
          />
        </template>
      </div>

      <div v-else class="task-result">
        <template v-if="task.taskKind === 'MEASUREMENT' && task.measuredValue !== null">
          Result: <strong>{{ task.measuredValue }} {{ task.measurementUnit }}</strong>
        </template>
        <template v-else-if="task.taskKind === 'VERIFICATION' && task.verificationResult">
          Result: <strong>{{ task.verificationResult.replace('_', ' ') }}</strong>
        </template>
        <template v-else-if="task.taskKind === 'TEXT_ENTRY' && task.enteredText">
          Notes: <strong>{{ task.enteredText }}</strong>
        </template>
      </div>
    </div>

    <span class="task-status" :class="{ 'status-pending': task.executionStatus === 'PENDING' }">
      {{ statusLabel }}
    </span>
  </div>
</template>

<style scoped>
.task-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 0;
  border-bottom: 1px solid var(--color-border-muted);
  position: relative;
}

.task-item:last-child {
  border-bottom: none;
}

.task-indicator {
  width: 4px;
  height: 100%;
  min-height: 2rem;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 0.125rem;
  transition: background-color 0.2s;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.task-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.task-badge {
  font-size: 0.625rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-muted);
}

.task-details {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.task-input-area {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-muted);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text-primary);
}

.task-select,
.task-input {
  width: 100%;
  max-width: 300px;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 0.375rem;
  background: var(--color-white);
  font-family: var(--font-sans);
  font-size: 0.875rem;
  color: var(--color-text-primary);
  transition: border-color 0.15s;
}

.task-select:focus,
.task-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(0, 94, 184, 0.2);
}

.input-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  max-width: 200px;
}

.input-unit {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.input-hint {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
}

.task-result {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.task-status {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  white-space: nowrap;
  margin-left: auto;
}

.task-status.status-pending {
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px dashed var(--color-border-muted);
}

.is-completed .task-title {
  text-decoration: line-through;
  color: var(--color-text-secondary);
}
</style>
