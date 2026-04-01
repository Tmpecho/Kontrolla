<script setup lang="ts">
import { computed } from 'vue'

import { deviationsByService } from '@/deviations/model/deviation.mock'
import type {
  DeviationServiceArea,
  DeviationSeverity,
  DeviationStatus,
} from '@/deviations/model/deviation.types'
import BaseButton from '@/shared/components/BaseButton.vue'

const props = defineProps<{
  serviceArea: DeviationServiceArea
  deviationPageTo: string
  addDeviationTo: string
}>()

const recentDeviations = computed(() => {
  return [...deviationsByService[props.serviceArea]]
    .sort((left, right) => new Date(right.reportedAt).getTime() - new Date(left.reportedAt).getTime())
    .slice(0, 2)
})

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatSeverity(severity: DeviationSeverity) {
  return severity.toLowerCase()
}

function formatStatus(status: DeviationStatus) {
  return status.toLowerCase().replace('_', ' ')
}
</script>

<template>
  <div class="deviations-tile">
    <div class="tile-header">
      <div>
        <h2>Recent deviations</h2>
        <p class="tile-subtitle">Latest reported issues that need follow-up.</p>
      </div>
      <RouterLink :to="deviationPageTo" class="tile-link">View all</RouterLink>
    </div>

    <ul class="recent-deviation-list">
      <li v-for="deviation in recentDeviations" :key="deviation.id" class="recent-deviation-item">
        <div class="recent-deviation-header">
          <p class="recent-deviation-title">{{ deviation.title }}</p>
          <div class="deviation-tags">
            <span :data-tone="formatSeverity(deviation.severity)" class="deviation-tag">
              {{ formatSeverity(deviation.severity) }}
            </span>
            <span :data-tone="formatStatus(deviation.status)" class="deviation-tag">
              {{ formatStatus(deviation.status) }}
            </span>
          </div>
        </div>
        <p class="recent-deviation-meta">
          {{ deviation.category }} · {{ formatDateTime(deviation.reportedAt) }}
        </p>
      </li>
    </ul>

    <RouterLink :to="addDeviationTo" class="add-link">
      <BaseButton type="button" class="add-btn">
        Add deviation
      </BaseButton>
    </RouterLink>
  </div>
</template>

<style scoped>
.deviations-tile {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
}

.tile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.tile-header h2,
.tile-subtitle,
.recent-deviation-title,
.recent-deviation-meta {
  margin: 0;
}

.tile-subtitle,
.recent-deviation-meta {
  color: var(--color-text-secondary);
}

.tile-subtitle {
  margin-top: 4px;
}

.tile-link {
  color: var(--color-primary);
  font-size: 0.875rem;
  text-decoration: none;
  white-space: nowrap;
}

.tile-link:hover {
  text-decoration: underline;
}

.recent-deviation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.recent-deviation-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
}

.recent-deviation-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.recent-deviation-title {
  font-weight: 600;
}

.deviation-tags {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.deviation-tag {
  display: inline-flex;
  align-self: flex-start;
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.deviation-tag[data-tone='low'],
.deviation-tag[data-tone='resolved'] {
  color: var(--color-success);
}

.deviation-tag[data-tone='medium'],
.deviation-tag[data-tone='in progress'] {
  color: var(--color-warning);
}

.deviation-tag[data-tone='high'],
.deviation-tag[data-tone='open'] {
  color: var(--color-primary);
}

.deviation-tag[data-tone='critical'] {
  color: var(--color-critical);
}

.add-btn {
  align-self: flex-start;
  width: auto;
  font-weight: 600;
}

.add-link {
  align-self: flex-start;
  text-decoration: none;
}
</style>
