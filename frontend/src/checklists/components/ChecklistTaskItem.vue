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
  if (isTaskIssue.value) {
    return 'var(--color-critical)'
  }
  if (props.editable && props.task.executionStatus === 'PENDING') {
    return 'var(--color-border-muted)'
  }
  return STATUS_COLORS[props.task.executionStatus] ?? 'var(--color-text-secondary)'
})

const isMeasurementOutOfRange = computed(() => {
  if (props.task.taskKind !== 'MEASUREMENT' || props.task.measuredValue === null) {
    return false
  }

  if (
    props.task.minimumAllowedValue !== null &&
    props.task.measuredValue < props.task.minimumAllowedValue
  ) {
    return true
  }

  if (
    props.task.maximumAllowedValue !== null &&
    props.task.measuredValue > props.task.maximumAllowedValue
  ) {
    return true
  }

  return false
})

const isTaskIssue = computed(
  () =>
    props.task.verificationResult === 'NOT_VERIFIED' || isMeasurementOutOfRange.value,
)

const issueLabel = computed(() => {
  if (props.task.verificationResult === 'NOT_VERIFIED') {
    return 'Issue'
  }

  if (isMeasurementOutOfRange.value) {
    return 'Out of range'
  }

  return null
})

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
  <div
    class="task-item"
    :class="{
      'is-completed': task.executionStatus === 'COMPLETED',
      'task-item-issue': isTaskIssue,
    }"
  >
    <div class="task-indicator" :style="{ backgroundColor: statusColor }" />

    <div class="task-content">
      <div class="task-title-row">
        <span class="task-title">{{ task.title }}</span>
        <span v-if="task.required" class="task-required-indicator" aria-label="Required task">
          *
        </span>
        <span v-if="issueLabel" class="task-issue-badge">{{ issueLabel }}</span>
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
              :value="task.measuredValue ?? ''"
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
          <strong>{{ task.measuredValue }} {{ task.measurementUnit }}</strong>
        </template>
        <template v-else-if="task.taskKind === 'VERIFICATION' && task.verificationResult">
          <strong>{{ task.verificationResult.replace('_', ' ') }}</strong>
        </template>
        <template v-else-if="task.taskKind === 'TEXT_ENTRY' && task.enteredText">
          <strong>{{ task.enteredText }}</strong>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-item {
  display: grid;
  grid-template-columns: 3px minmax(14rem, 26rem) minmax(10.5rem, 12.5rem);
  align-items: start;
  column-gap: 0.5rem;
  row-gap: 0.125rem;
  padding: 0.55rem 0.35rem;
  border-bottom: 1px solid color-mix(in srgb, var(--color-border-muted) 78%, white);
  border-radius: var(--radius-fluid-sm);
  background: color-mix(in srgb, var(--color-surface) 28%, white);
  position: relative;
}
.task-item-issue {
  background: color-mix(in srgb, var(--color-critical) 7%, white);
  border-bottom-color: color-mix(in srgb, var(--color-critical) 28%, white);
}
.task-item:last-child {
  border-bottom: none;
}
.task-indicator {
  width: 3px;
  min-height: 1.4rem;
  border-radius: var(--radius-fluid-sm);
  margin-top: 0.125rem;
  transition: background-color 0.2s;
}
.task-content {
  display: flex;
  flex-direction: column;
  gap: 0.16rem;
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
.task-item-issue .task-title,
.task-item-issue .task-result strong {
  color: var(--color-critical);
}
.task-item-issue .task-details,
.task-item-issue .input-hint {
  color: color-mix(in srgb, var(--color-critical) 72%, var(--color-text-secondary));
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
.task-issue-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.08rem 0.32rem;
  border: 1px solid color-mix(in srgb, var(--color-critical) 35%, white);
  border-radius: var(--radius-fluid-sm);
  background: color-mix(in srgb, var(--color-critical) 10%, white);
  color: var(--color-critical);
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.task-control {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
  gap: 0.22rem;
  min-width: 0;
  align-self: start;
  width: 100%;
  padding: 0 0 0 0.55rem;
  border-left: 1px solid color-mix(in srgb, var(--color-border-muted) 72%, white);
}
.task-item-issue .task-control {
  border-left-color: color-mix(in srgb, var(--color-critical) 28%, white);
}

.task-input-area {
  display: grid;
  gap: 0.22rem;
  justify-items: stretch;
  width: 100%;
}
.task-input {
  width: 100%;
  max-width: 100%;
  padding: 0.38rem 0.55rem;
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-fluid-sm);
  background: var(--color-white);
  font: 400 0.76rem var(--font-sans, inherit);
  color: var(--color-text-primary);
  transition: 0.15s;
}
.task-item-issue .task-input {
  border-color: color-mix(in srgb, var(--color-critical) 28%, white);
  background: color-mix(in srgb, var(--color-critical) 3%, white);
}
.task-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 20%, transparent);
}
.input-group {
  width: 100%;
  max-width: 100%;
  justify-content: flex-start;
  min-width: 0;
}

.input-group .task-input {
  flex: 1 1 auto;
  min-width: 0;
}
.input-unit {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  flex: 0 0 auto;
}
.input-hint {
  font-size: 0.625rem;
  color: var(--color-text-secondary);
  text-align: left;
}
.task-result {
  text-align: left;
  width: 100%;
  min-height: 1.75rem;
  display: flex;
  align-items: center;
}
.task-item-issue .task-result {
  color: var(--color-critical);
}
.task-result strong {
  color: var(--color-text-primary);
  font-weight: 600;
}

@media (max-width: 720px) {
  .task-item {
    grid-template-columns: 3px minmax(0, 1fr);
    align-items: start;
    padding: 0.5rem 0;
    background: transparent;
    border-radius: var(--radius-sharp);
  }

  .task-control {
    grid-column: 2;
    justify-content: flex-start;
    margin-top: 0.15rem;
    align-self: start;
    padding: 0;
    border-left: 0;
    width: 100%;
  }

  .task-input-area {
    width: 100%;
  }

  .input-group {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 0.45rem;
  }

  .task-input-area,
  .task-result,
  .input-hint {
    text-align: left;
    max-width: 100%;
  }
}
</style>
