<script setup lang="ts">
import type { ChecklistTaskExecution } from '@/checklists/model/checklist.types'
import { computed } from 'vue'

const props = defineProps<{
  task: ChecklistTaskExecution
  editable?: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle', id: string): void
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
  return props.task.executionStatus.charAt(0) + props.task.executionStatus.slice(1).toLowerCase()
})

function handleClick() {
  if (props.editable && props.task.executionStatus !== 'SKIPPED') {
    emit('toggle', props.task.checklistTaskExecutionId)
  }
}
</script>

<template>
  <div
    class="task-item"
    :class="{ 'is-interactive': editable && task.executionStatus !== 'SKIPPED' }"
    @click="handleClick"
  >
    <!-- Status Indicator Bar -->
    <div class="task-indicator" :style="{ backgroundColor: statusColor }" />

    <!-- Checkbox (Only shown when editable) -->
    <div
      v-if="editable"
      class="task-checkbox"
      :class="{ checked: task.executionStatus === 'COMPLETED' }"
    >
      <svg
        v-if="task.executionStatus === 'COMPLETED'"
        width="12"
        height="12"
        viewBox="0 0 12 12"
        fill="none"
      >
        <path
          d="M2 6L5 9L10 3"
          stroke="white"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>

    <!-- Main Content -->
    <div class="task-content">
      <span class="task-title">{{ task.title }}</span>
      <span v-if="task.details" class="task-details">{{ task.details }}</span>
    </div>

    <!-- Status Badge -->
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
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border-muted);
  position: relative;
}

.task-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.task-indicator {
  width: 4px;
  height: 100%;
  min-height: 1.5rem;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.task-checkbox {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border-muted);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
  margin-top: 0.125rem; /* Align with title line */
}

.task-checkbox.checked {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0; /* Prevent text overflow */
}

.task-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.task-details {
  font-size: 0.8125rem;
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
  color: var(--color-text-secondary);
  background: transparent;
}

/* Interaction Styles */
.task-item.is-interactive {
  cursor: pointer;
  transition: background-color 0.1s ease;
  border-radius: 4px;
  margin: 0 -0.5rem; /* Expand hover area to edges */
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.task-item.is-interactive:hover {
  background-color: var(--color-surface);
}
</style>
