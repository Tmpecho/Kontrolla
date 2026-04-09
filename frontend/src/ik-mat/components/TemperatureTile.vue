<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import { listTemperatureUnits } from '@/ik-mat/api/temperature.api'
import type { TemperatureUnitListItem } from '@/ik-mat/model/temperature.types'
import {
  formatTemperatureAlertState,
  formatTemperatureUnitType,
  getTemperatureUnitsWithStatus,
} from '@/ik-mat/model/temperature.utils'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

defineProps<{
  temperaturePageTo: string
}>()

const authStore = useAuthStore()
const units = ref<TemperatureUnitListItem[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(() => {
  if (authStore.appContext?.organizationId) {
    return authStore.appContext.establishmentId ?? null
  }

  return appEnv.defaultEstablishmentId ?? null
})

const missingContextMessage = computed(() => {
  if (organizationId.value && establishmentId.value) {
    return null
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose an establishment to load temperature units.'
  }

  if (!appEnv.isDevelopment) {
    return 'Temperature logs are unavailable until organization context is ready.'
  }

  return 'Set the default organization and establishment IDs or sign in with an organization context to load temperature units.'
})

const highlightedUnits = computed(() => {
  return [...units.value]
    .sort((left, right) => {
      const alertOrder = getAlertSortOrder(left.alertState) - getAlertSortOrder(right.alertState)
      if (alertOrder !== 0) {
        return alertOrder
      }

      return left.nextDueAt.getTime() - right.nextDueAt.getTime()
    })
    .slice(0, 2)
})

function getAlertSortOrder(alertState: TemperatureUnitListItem['alertState']): number {
  switch (alertState) {
    case 'OUT_OF_RANGE':
      return 0
    case 'OVERDUE':
      return 1
    case 'DUE_SOON':
      return 2
    case 'NO_READING':
      return 3
    case 'DUE_LATER_TODAY':
      return 4
    default:
      return 5
  }
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDueTime(value: Date) {
  return new Intl.DateTimeFormat('nb-NO', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function formatMeta(unit: TemperatureUnitListItem) {
  if (unit.latestLog) {
    return `${formatTemperatureUnitType(unit.type)} · ${formatDateTime(unit.latestLog.measuredAt)}`
  }

  return `${formatTemperatureUnitType(unit.type)} · Due by ${formatDueTime(unit.nextDueAt)}`
}

function getAccent(alertState: TemperatureUnitListItem['alertState']) {
  switch (alertState) {
    case 'OUT_OF_RANGE':
      return 'critical'
    case 'OVERDUE':
      return 'medium'
    case 'DUE_SOON':
      return 'high'
    case 'NO_READING':
      return 'medium'
    default:
      return 'low'
  }
}

async function loadUnits(): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    units.value = []
    errorMessage.value = null
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const fetchedUnits = await listTemperatureUnits({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
    })
    units.value = getTemperatureUnitsWithStatus(fetchedUnits)
  } catch (error) {
    units.value = []
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load temperature units.'
  } finally {
    isLoading.value = false
  }
}

watch([organizationId, establishmentId], () => {
  void loadUnits()
}, { immediate: true })
</script>

<template>
  <div class="temperature-tile">
    <div class="tile-header">
      <div>
        <h2>Temperature</h2>
        <p class="tile-subtitle">Recent readings and units that need follow-up.</p>
      </div>
      <RouterLink :to="temperaturePageTo" class="tile-link">View all</RouterLink>
    </div>

    <p v-if="missingContextMessage" class="temperature-meta">{{ missingContextMessage }}</p>
    <p v-else-if="isLoading" class="temperature-meta">Loading temperature units...</p>
    <p v-else-if="errorMessage" class="temperature-meta">{{ errorMessage }}</p>
    <p v-else-if="highlightedUnits.length === 0" class="temperature-meta">
      No temperature units found.
    </p>

    <ul v-else class="temperature-list">
      <li v-for="unit in highlightedUnits" :key="unit.id" class="temperature-item">
        <RouterLink
          :to="temperaturePageTo"
          :data-accent="getAccent(unit.alertState)"
          class="temperature-link"
        >
          <div class="temperature-header">
            <p class="temperature-title">{{ unit.name }}</p>
            <div class="temperature-tags">
              <span class="temperature-tag">
                {{ formatTemperatureUnitType(unit.type) }}
              </span>
              <span :data-tone="getAccent(unit.alertState)" class="temperature-tag">
                {{ formatTemperatureAlertState(unit.alertState) }}
              </span>
            </div>
          </div>
          <p class="temperature-meta">
            {{ unit.location }} · {{ formatMeta(unit) }}
          </p>
        </RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.temperature-tile {
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
.temperature-title,
.temperature-meta {
  margin: 0;
}

.tile-subtitle,
.temperature-meta {
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

.temperature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.temperature-item {
  display: flex;
}

.temperature-link {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: inherit;
  text-decoration: none;
  overflow: hidden;
  transition:
    border-color 120ms ease,
    background-color 120ms ease;
}

.temperature-link::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 4px;
  background-color: var(--color-border-muted);
}

.temperature-link[data-accent='low']::before {
  background-color: var(--color-success);
}

.temperature-link[data-accent='high']::before {
  background-color: var(--color-primary);
}

.temperature-link[data-accent='medium']::before {
  background-color: var(--color-warning);
}

.temperature-link[data-accent='critical']::before {
  background-color: var(--color-critical);
}

.temperature-link:hover {
  border-color: var(--color-primary);
  background-color: var(--color-container);
}

.temperature-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.temperature-title {
  font-weight: 600;
}

.temperature-tags {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.temperature-tag {
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

.temperature-tag[data-tone='low'] {
  color: var(--color-success);
}

.temperature-tag[data-tone='high'] {
  color: var(--color-primary);
}

.temperature-tag[data-tone='medium'] {
  color: var(--color-warning);
}

.temperature-tag[data-tone='critical'] {
  color: var(--color-critical);
}

</style>
