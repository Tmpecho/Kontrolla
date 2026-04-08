<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import ChecklistRunCard from '@/checklists/components/ChecklistRunCard.vue'
import { selectLatestChecklistRuns } from '@/checklists/model/checklist-runs.utils'
import type { ChecklistRun, ChecklistRunStatus } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

type TriageFilter = 'OVERDUE' | 'DUE_TODAY' | 'IN_PROGRESS' | 'COMPLETED'

const checklistRuns = ref<ChecklistRun[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const activeFilter = ref<TriageFilter>('OVERDUE')
const searchQuery = ref('')
const route = useRoute()
const authStore = useAuthStore()

const ACTIVE_STATUSES: ChecklistRunStatus[] = ['PENDING', 'OVERDUE', 'IN_PROGRESS']

const resolvedChecklistContext = computed(() => {
  if (!authStore.isSessionReady) {
    return null
  }

  const organizationId = authStore.appContext?.organizationId ?? null
  const establishmentId = authStore.appContext?.establishmentId ?? null

  if (organizationId && establishmentId) {
    return { organizationId, establishmentId }
  }

  if (!authStore.isAuthenticated) {
    const defaultOrganizationId = appEnv.defaultOrganizationId
    const defaultEstablishmentId = appEnv.defaultEstablishmentId

    if (defaultOrganizationId && defaultEstablishmentId) {
      return {
        organizationId: defaultOrganizationId,
        establishmentId: defaultEstablishmentId,
      }
    }
  }

  return null
})

const hasChecklistContext = computed(
  () => resolvedChecklistContext.value !== null,
)

const missingContextMessage = computed(() => {
  if (!authStore.isSessionReady) {
    return 'Loading checklist context...'
  }

  if (hasChecklistContext.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Checklist runs cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID to load checklist runs in development.'
})

async function loadChecklistRuns(): Promise<void> {
  const context = resolvedChecklistContext.value

  if (!context) {
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const page = await listChecklistRuns({
      organizationId: context.organizationId,
      establishmentId: context.establishmentId,
      serviceArea: 'IK_MAT',
      size: 10,
    })

    checklistRuns.value = selectLatestChecklistRuns(page.items)
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load checklist runs.'
  } finally {
    isLoading.value = false
  }
}

function handleRunUpdate(updatedRun: ChecklistRun) {
  checklistRuns.value = checklistRuns.value.map((run) =>
    run.id === updatedRun.id ? updatedRun : run,
  )
}

function handleFilterSelect(filter: TriageFilter) {
  activeFilter.value = filter
  if (hasSearchQuery.value) {
    searchQuery.value = ''
  }
}

function isOverdue(run: ChecklistRun): boolean {
  if (!ACTIVE_STATUSES.includes(run.status) || run.status === 'IN_PROGRESS') {
    return false
  }

  return new Date(run.dueAt).getTime() < new Date().getTime()
}

function isDueToday(run: ChecklistRun): boolean {
  if (!ACTIVE_STATUSES.includes(run.status) || run.status === 'IN_PROGRESS' || isOverdue(run)) {
    return false
  }

  const dueDate = new Date(run.dueAt)
  const now = new Date()

  return (
    dueDate.getFullYear() === now.getFullYear() &&
    dueDate.getMonth() === now.getMonth() &&
    dueDate.getDate() === now.getDate() &&
    dueDate.getTime() >= now.getTime()
  )
}

function matchesFilter(run: ChecklistRun, filter: TriageFilter): boolean {
  switch (filter) {
    case 'OVERDUE':
      return isOverdue(run)
    case 'DUE_TODAY':
      return isDueToday(run)
    case 'IN_PROGRESS':
      return run.status === 'IN_PROGRESS'
    case 'COMPLETED':
      return run.status === 'COMPLETED'
  }
}

const triageOptions = computed(() => {
  const options: Array<{ key: TriageFilter; label: string; count: number }> = [
    { key: 'OVERDUE', label: 'Overdue', count: 0 },
    { key: 'DUE_TODAY', label: 'Due today', count: 0 },
    { key: 'IN_PROGRESS', label: 'In progress', count: 0 },
    { key: 'COMPLETED', label: 'Completed', count: 0 },
  ]

  return options.map((option) => ({
    ...option,
    count: checklistRuns.value.filter((run) => matchesFilter(run, option.key)).length,
  }))
})

const hasSearchQuery = computed(() => searchQuery.value.trim().length > 0)
const selectedChecklistRunId = computed(() => {
  const routeQueryValue = route.query.checklistRunId
  return typeof routeQueryValue === 'string' && routeQueryValue.length > 0 ? routeQueryValue : null
})
const isFilterVisuallyActive = (filter: TriageFilter) =>
  !hasSearchQuery.value && activeFilter.value === filter

const filteredChecklistRuns = computed(() => {
  const normalizedQuery = searchQuery.value.trim().toLowerCase()

  return checklistRuns.value.filter((run) => {
    const haystack = [run.title, run.description ?? '', run.status.replace(/_/g, ' ')]
      .join(' ')
      .toLowerCase()

    if (normalizedQuery) {
      return haystack.includes(normalizedQuery)
    }

    return matchesFilter(run, activeFilter.value) || selectedChecklistRunId.value === run.id
  })
})

watch(
  resolvedChecklistContext,
  async (context) => {
    if (!context) {
      checklistRuns.value = []
      errorMessage.value = null
      isLoading.value = false
      return
    }

    await loadChecklistRuns()
  },
  { immediate: true },
)
</script>

<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <h1>IK-mat Checklists</h1>
        <p>Scheduled routines and task progress for food compliance.</p>
      </div>
    </header>

    <div v-if="missingContextMessage" class="state-card">
      <p>{{ missingContextMessage }}</p>
    </div>

    <div v-else-if="isLoading" class="state-card">
      <p>Loading checklist runs...</p>
    </div>

    <div v-else-if="errorMessage" class="state-card state-card-error">
      <p>{{ errorMessage }}</p>
    </div>

    <template v-else-if="checklistRuns.length > 0">
      <section class="triage-bar" aria-label="Checklist triage">
        <div class="triage-tabs" role="tablist" aria-label="Checklist status filters">
          <button
            v-for="option in triageOptions"
            :key="option.key"
            class="triage-tab"
            :class="{ 'triage-tab-active': isFilterVisuallyActive(option.key) }"
            type="button"
            :aria-selected="isFilterVisuallyActive(option.key)"
            @click="handleFilterSelect(option.key)"
          >
            <span>{{ option.label }}</span>
            <span class="triage-count">{{ option.count }}</span>
          </button>
        </div>

        <label class="search-field">
          <span class="sr-only">Search checklists</span>
          <input
            v-model="searchQuery"
            type="search"
            name="checklist-search"
            placeholder="Search checklists"
          />
        </label>
      </section>

      <div v-if="filteredChecklistRuns.length > 0" class="runs-grid">
        <ChecklistRunCard
          v-for="run in filteredChecklistRuns"
          :key="run.id"
          :run="run"
          :organization-id="resolvedChecklistContext!.organizationId"
          :establishment-id="resolvedChecklistContext!.establishmentId"
          :selected="selectedChecklistRunId === run.id"
          :force-expanded="selectedChecklistRunId === run.id"
          @update:run="handleRunUpdate"
        />
      </div>

      <div v-else class="state-card">
        <p>{{ hasSearchQuery ? 'No checklist runs match your search.' : 'No checklist runs match the current filter.' }}</p>
      </div>
    </template>

    <div v-else class="state-card">
      <p>No checklist runs found.</p>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header h1,
.page-header p,
.state-card p {
  margin: 0;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header p {
  margin-top: 8px;
  color: var(--color-text-secondary);
}

.triage-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.triage-tabs {
  display: flex;
  gap: 0;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--color-border-muted);
}

.triage-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  min-height: 3rem;
  padding: 0.75rem 1rem 0.6875rem;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-text-secondary);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition:
    color 120ms ease,
    border-color 120ms ease,
    background-color 120ms ease;
}

.triage-tab:hover {
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.triage-tab-active {
  border-color: var(--color-primary);
  background: var(--color-container);
  color: var(--color-text-primary);
}

.triage-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  height: 1.5rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-border-muted) 70%, white);
  color: inherit;
  font-size: 0.8125rem;
  font-weight: 700;
}

.search-field {
  min-width: min(100%, 320px);
}

.search-field input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border-muted);
  border-radius: 999px;
  background: var(--color-container);
  color: var(--color-text-primary);
  font: inherit;
}

.search-field input::placeholder {
  color: var(--color-text-secondary);
}

.runs-grid {
  display: grid;
  gap: 16px;
}

.state-card {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-secondary);
}

.state-card-error {
  color: var(--color-critical);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
