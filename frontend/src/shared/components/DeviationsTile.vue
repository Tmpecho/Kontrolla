<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import { listEstablishmentDeviations } from '@/deviations/api/deviations.api'
import type {
  DeviationServiceArea,
  DeviationCategoryValue,
  DeviationSeverity,
  DeviationStatus,
} from '@/deviations/model/deviation.types'
import {
  formatDeviationStatus as formatStatus,
  formatDeviationSeverity as formatSeverity,
  getDeviationServiceAreaForCategory,
  toDeviationCategoryLabel,
} from '@/deviations/model/deviation.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'
import BaseButton from '@/shared/components/BaseButton.vue'

const props = defineProps<{
  serviceArea: DeviationServiceArea
  deviationPageTo: string
  addDeviationTo: string
}>()

type TileDeviation = {
  id: string
  title: string
  reportedAt: string
  category: DeviationCategoryValue
  severity: DeviationSeverity
  status: DeviationStatus
}

const authStore = useAuthStore()
const deviations = ref<TileDeviation[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(
  () => authStore.appContext?.establishmentId ?? appEnv.defaultEstablishmentId ?? null,
)

const missingContextMessage = computed(() => {
  if (organizationId.value && establishmentId.value) {
    return null
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose an establishment to load deviations.'
  }

  if (!appEnv.isDevelopment) {
    return 'Deviations are unavailable until organization context is ready.'
  }

  return 'Set the default organization and establishment IDs or sign in with an organization context to load deviations.'
})

const recentDeviations = computed(() => {
  return deviations.value
    .filter(
      (deviation) =>
        getDeviationServiceAreaForCategory(toDeviationCategoryLabel(deviation.category)) ===
        props.serviceArea,
    )
    .sort((left, right) => new Date(right.reportedAt).getTime() - new Date(left.reportedAt).getTime())
    .slice(0, 2)
})

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatCategory(category: DeviationCategoryValue) {
  return toDeviationCategoryLabel(category)
}

function getDeviationLink(deviationId: string) {
  return {
    path: props.deviationPageTo,
    query: {
      deviationId,
    },
  }
}

async function loadDeviations(): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    deviations.value = []
    errorMessage.value = null
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const page = await listEstablishmentDeviations({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      size: 10,
    })

    deviations.value = page.items.map((deviation) => ({
      id: deviation.id,
      title: deviation.title,
      reportedAt: deviation.createdAt,
      category: deviation.category,
      severity: deviation.severity,
      status: deviation.status,
    }))
  } catch (error) {
    deviations.value = []
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load deviations.'
  } finally {
    isLoading.value = false
  }
}

watch([organizationId, establishmentId], () => {
  void loadDeviations()
}, { immediate: true })
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

    <p v-if="missingContextMessage" class="recent-deviation-meta">{{ missingContextMessage }}</p>
    <p v-else-if="isLoading" class="recent-deviation-meta">Loading deviations...</p>
    <p v-else-if="errorMessage" class="recent-deviation-meta">{{ errorMessage }}</p>
    <p v-else-if="recentDeviations.length === 0" class="recent-deviation-meta">
      No deviations found.
    </p>

    <ul v-else class="recent-deviation-list">
      <li v-for="deviation in recentDeviations" :key="deviation.id" class="recent-deviation-item">
        <RouterLink
          :to="getDeviationLink(deviation.id)"
          :data-accent="formatSeverity(deviation.severity)"
          class="recent-deviation-link"
        >
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
            {{ formatCategory(deviation.category) }} · {{ formatDateTime(deviation.reportedAt) }}
          </p>
        </RouterLink>
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
}

.recent-deviation-link {
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

.recent-deviation-link::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 4px;
  background-color: var(--color-border-muted);
}

.recent-deviation-link[data-accent='low']::before {
  background-color: var(--color-success);
}

.recent-deviation-link[data-accent='medium']::before {
  background-color: var(--color-warning);
}

.recent-deviation-link[data-accent='high']::before {
  background-color: var(--color-primary);
}

.recent-deviation-link[data-accent='critical']::before {
  background-color: var(--color-critical);
}

.recent-deviation-link:hover {
  border-color: var(--color-primary);
  background-color: var(--color-container);
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
