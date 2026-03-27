<script setup lang="ts">
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import ChecklistTaskItem from './ChecklistTaskItem.vue'
import { computed } from 'vue'

const props = defineProps<{
  run: ChecklistRun
}>()

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

    <!-- Nested Task List -->
    <div class="tasks-container">
      <h4 class="tasks-heading">Tasks ({{ run.tasks.length }})</h4>
      <div v-if="run.tasks.length > 0" class="tasks-list">
        <ChecklistTaskItem
          v-for="task in run.tasks"
          :key="task.checklistTaskExecutionId"
          :task="task"
        />
      </div>
      <p v-else class="empty-text">No tasks defined for this run.</p>
    </div>
  </article>
</template>

<style scoped>
.run-card {
  background-color: var(--color-container);
  border-radius: 0.75rem;
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

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

.run-meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
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
</style>
