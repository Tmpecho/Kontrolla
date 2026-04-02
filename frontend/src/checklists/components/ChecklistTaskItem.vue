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

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: 'var(--color-success)',
  SKIPPED: 'var(--color-warning)',
}

const statusColor = computed(() => {
  if (props.editable && props.task.executionStatus === 'PENDING') {
    return 'var(--color-border-muted)'
  }
  return STATUS_COLORS[props.task.executionStatus] ?? 'var(--color-text-secondary)'
})

const statusLabel = computed(() =>
  props.task.executionStatus === 'PENDING'
    ? 'Pending'
    : props.task.executionStatus.charAt(0) + props.task.executionStatus.slice(1).toLowerCase(),
)

const updateTask = (fields: Partial<ChecklistTaskExecution>) => {
  emit('update:task', { ...props.task, ...fields })
}

const resolveCompletion = (hasValue: boolean) => ({
  executionStatus: (hasValue
    ? 'COMPLETED'
    : 'PENDING') as ChecklistTaskExecution['executionStatus'],
  resolvedAt: hasValue ? new Date().toISOString() : null,
})

const extractValue = (e: Event) => (e.target as HTMLInputElement | HTMLSelectElement).value

const toggleAction = () => {
  const isCompleting = props.task.executionStatus !== 'COMPLETED'
  updateTask(resolveCompletion(isCompleting))
}

const handleVerificationChange = (e: Event) => {
  const value = extractValue(e) as ChecklistVerificationResult | ''
  updateTask({
    verificationResult: value || null,
    ...resolveCompletion(!!value),
  })
}

const handleMeasurementChange = (e: Event) => {
  const value = extractValue(e)
  updateTask({
    measuredValue: value === '' ? null : parseFloat(value),
    ...resolveCompletion(value !== ''),
  })
}

const handleTextChange = (e: Event) => {
  const value = extractValue(e).trim()
  updateTask({
    enteredText: value || null,
    ...resolveCompletion(!!value),
  })
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
        <!-- ACTION -->
        <label v-if="task.taskKind === 'ACTION'" class="checkbox-label">
          <input
            type="checkbox"
            :checked="task.executionStatus === 'COMPLETED'"
            @change="toggleAction"
          />
          <span>Mark as Done</span>
        </label>

        <!-- VERIFICATION -->
        <select
          v-else-if="task.taskKind === 'VERIFICATION'"
          :value="task.verificationResult || ''"
          @change="handleVerificationChange"
          class="task-input"
        >
          <option value="">Select Result...</option>
          <option value="VERIFIED">Verified (OK)</option>
          <option value="NOT_VERIFIED">Not Verified (Issue)</option>
        </select>

        <!-- MEASUREMENT -->
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

        <!-- TEXT ENTRY -->
        <input
          v-else-if="task.taskKind === 'TEXT_ENTRY'"
          type="text"
          :value="task.enteredText || ''"
          @change="handleTextChange"
          class="task-input"
          placeholder="Enter notes..."
        />
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
  min-height: 2rem;
  border-radius: 1cqh;
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
.task-header,
.checkbox-label,
.input-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.task-title {
  font: 500 0.9375rem/1.4 var(--font-sans, inherit);
  color: var(--color-text-primary);
}
.is-completed .task-title {
  text-decoration: line-through;
  color: var(--color-text-secondary);
}
.task-details,
.task-result {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}
.checkbox-label {
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text-primary);
}

.task-badge,
.task-status {
  font-weight: 700;
  text-transform: uppercase;
  border-radius: 1cqh;
}
.task-badge {
  font-size: 0.625rem;
  padding: 0.125rem 0.375rem;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-muted);
}
.task-status {
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: 0.25rem 0.5rem;
  white-space: nowrap;
  margin-left: auto;
}
.task-status.status-pending {
  background: transparent;
  border: 1px dashed var(--color-border-muted);
}

.task-input-area {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-muted);
}
.task-input {
  width: 100%;
  max-width: 300px;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 1cqh;
  background: var(--color-white);
  font: 400 0.875rem var(--font-sans, inherit);
  color: var(--color-text-primary);
  transition: 0.15s;
}
.task-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(0, 94, 184, 0.2);
}
.input-group {
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
</style>
