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
      <div class="task-title-row">
        <span class="task-title">{{ task.title }}</span>
        <span v-if="task.required" class="task-required-indicator" aria-label="Required task">
          *
        </span>
      </div>
      <span v-if="task.details" class="task-details">{{ task.details }}</span>
    </div>

    <div class="task-control">
      <div v-if="editable" class="task-input-area">
        <label v-if="task.taskKind === 'ACTION'" class="checkbox-label">
          <input
            type="checkbox"
            :checked="task.executionStatus === 'COMPLETED'"
            @change="toggleAction"
          />
          <span>Done</span>
        </label>

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
  </div>
</template>

<style scoped>
.task-item {
  display: grid;
  grid-template-columns: 3px minmax(0, 1fr) minmax(10rem, 15rem);
  align-items: start;
  column-gap: 0.6rem;
  row-gap: 0.125rem;
  padding: 0.55rem 0;
  border-bottom: 1px solid var(--color-border-muted);
  position: relative;
}
.task-item:last-child {
  border-bottom: none;
}
.task-indicator {
  width: 3px;
  min-height: 1.4rem;
  border-radius: 0.5cqh;
  margin-top: 0.125rem;
  transition: background-color 0.2s;
}
.task-content {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}
.checkbox-label,
.input-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.task-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex-wrap: wrap;
}

.task-title {
  font: 500 0.8rem/1.25 var(--font-sans, inherit);
  color: var(--color-text-primary);
}
.is-completed .task-title {
  text-decoration: line-through;
  color: var(--color-text-secondary);
}
.task-details,
.task-result {
  font-size: 0.7rem;
  color: var(--color-text-secondary);
}
.checkbox-label {
  cursor: pointer;
  font-size: 0.76rem;
  color: var(--color-text-primary);
}
.task-required-indicator {
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-critical);
}
.task-control {
  display: flex;
  justify-content: flex-end;
  min-width: 0;
}

.task-input-area {
  display: grid;
  gap: 0.3rem;
  justify-items: stretch;
  width: 100%;
}
.task-input {
  width: 100%;
  max-width: 100%;
  padding: 0.38rem 0.55rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 0.75cqh;
  background: var(--color-white);
  font: 400 0.76rem var(--font-sans, inherit);
  color: var(--color-text-primary);
  transition: 0.15s;
}
.task-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(0, 94, 184, 0.2);
}
.input-group {
  width: 100%;
  max-width: 100%;
  justify-content: flex-end;
}
.input-unit {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.input-hint {
  font-size: 0.625rem;
  color: var(--color-text-secondary);
  text-align: right;
}
.task-result {
  text-align: right;
}

@media (max-width: 720px) {
  .task-item {
    grid-template-columns: 3px minmax(0, 1fr);
  }

  .task-control {
    grid-column: 2;
    justify-content: flex-start;
    margin-top: 0.15rem;
  }

  .task-input-area,
  .task-result,
  .input-hint {
    text-align: left;
  }
}
</style>
