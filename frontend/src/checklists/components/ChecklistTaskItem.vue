<script setup lang="ts">
import type { ChecklistTaskExecution } from '@/checklists/model/checklist.types'
import { computed } from 'vue'

const props = defineProps<{
  task: ChecklistTaskExecution
}>()

const statusColor = computed(() => {
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
</script>

<template>
  <div class="task-item">
    <div class="task-indicator" :style="{ backgroundColor: statusColor }" />
    <div class="task-content">
      <span class="task-title">{{ task.title }}</span>
      <span v-if="task.details" class="task-details">{{ task.details }}</span>
    </div>
    <span class="task-status">{{ statusLabel }}</span>
  </div>
</template>

<style scoped>
.task-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border-muted);
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

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
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
}
</style>
