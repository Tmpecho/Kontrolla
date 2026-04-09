<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import ChecklistDefinitionManager from '@/checklists/components/ChecklistDefinitionManager.vue'
import ChecklistRunCard from '@/checklists/components/ChecklistRunCard.vue'
import { selectLatestChecklistRuns } from '@/checklists/model/checklist-runs.utils'
import type { ChecklistRun, ChecklistRunStatus } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

type TriageFilter = 'UPCOMING' | 'LATE' | 'DUE_TODAY' | 'IN_PROGRESS' | 'COMPLETED'

const authStore = useAuthStore()
const route = useRoute()
const allChecklistRuns = ref<ChecklistRun[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const activeFilter = ref<TriageFilter>('UPCOMING')
const searchQuery = ref('')
const pinnedChecklistRunId = ref<string | null>(null)
const requestedDefinitionGroupId = ref<string | null>(null)

const ACTIVE_STATUSES: ChecklistRunStatus[] = ['PENDING', 'OVERDUE', 'IN_PROGRESS']
const canManageChecklistDefinitions = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN' ||
    authStore.appContext?.organizationRole === 'ORG_MANAGER'
  )
})

const resolvedChecklistContext = computed(() => {
  if (!authStore.isSessionReady) {
    return null
  }

  const organizationId = authStore.appContext?.organizationId ?? null

  if (organizationId) {
    const selectedEstablishmentId = authStore.appContext?.establishmentId ?? null

    if (selectedEstablishmentId) {
      return { organizationId, establishmentIds: [selectedEstablishmentId] }
    }

    const establishmentIds = (authStore.establishments ?? []).map((establishment) => establishment.id)
    if (establishmentIds.length > 0) {
      return { organizationId, establishmentIds }
    }
  }

  if (!authStore.isAuthenticated) {
    const defaultOrganizationId = appEnv.defaultOrganizationId
    const defaultEstablishmentId = appEnv.defaultEstablishmentId

    if (defaultOrganizationId && defaultEstablishmentId) {
      return {
        organizationId: defaultOrganizationId,
        establishmentIds: [defaultEstablishmentId],
      }
    }
  }

  return null
})

const hasChecklistContext = computed(() => resolvedChecklistContext.value !== null)
const selectedManagementEstablishmentId = computed(() => {
  const context = resolvedChecklistContext.value

  if (!context || context.establishmentIds.length !== 1) {
    return null
  }

  return context.establishmentIds[0] ?? null
})

const missingContextMessage = computed(() => {
  if (!authStore.isSessionReady) {
    return 'Loading checklist context...'
  }

  if (hasChecklistContext.value) {
    return null
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose an establishment to load checklist runs.'
  }

  if (!appEnv.isDevelopment) {
    return 'Checklist runs cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID to load checklist runs in development.'
})

async function loadChecklistRuns(): Promise<void> {
  const context = resolvedChecklistContext.value

  if (!context) {
    allChecklistRuns.value = []
    errorMessage.value = null
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const pages = await Promise.all(
      context.establishmentIds.map((establishmentId) =>
        listChecklistRuns({
          organizationId: context.organizationId,
          establishmentId,
          serviceArea: 'IK_MAT',
          size: 200,
        }),
      ),
    )

    allChecklistRuns.value = pages.flatMap((page) => page.items)
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load checklist runs.'
  } finally {
    isLoading.value = false
  }
}

async function handleDefinitionsSaved(): Promise<void> {
  await loadChecklistRuns()
}

function handleDefinitionEditRequest(definitionGroupId: string): void {
  requestedDefinitionGroupId.value = definitionGroupId
}

function clearDefinitionEditRequest(): void {
  requestedDefinitionGroupId.value = null
}

function handleRunUpdate(updatedRun: ChecklistRun) {
  allChecklistRuns.value = allChecklistRuns.value.map((run) =>
    run.id === updatedRun.id ? updatedRun : run,
  )

  if (selectedChecklistRunId.value || hasSearchQuery.value) {
    return
  }

  pinnedChecklistRunId.value = matchesFilter(updatedRun, activeFilter.value) ? null : updatedRun.id
}

function handleFilterSelect(filter: TriageFilter) {
  activeFilter.value = filter
  pinnedChecklistRunId.value = null
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

function isDueOnCurrentDate(run: ChecklistRun): boolean {
  if (!ACTIVE_STATUSES.includes(run.status) || run.status === 'IN_PROGRESS') {
    return false
  }

  const dueDate = new Date(run.dueAt)
  const now = new Date()

  return (
    dueDate.getFullYear() === now.getFullYear() &&
    dueDate.getMonth() === now.getMonth() &&
    dueDate.getDate() === now.getDate()
  )
}

function isDueToday(run: ChecklistRun): boolean {
  return isDueOnCurrentDate(run)
}

function isLate(run: ChecklistRun): boolean {
  if (run.status !== 'OVERDUE') {
    return false
  }

  return !isDueOnCurrentDate(run)
}

function isUpcoming(run: ChecklistRun): boolean {
  if (run.status !== 'PENDING') {
    return false
  }

  return !isDueToday(run)
}

function matchesFilter(run: ChecklistRun, filter: TriageFilter): boolean {
  switch (filter) {
    case 'UPCOMING':
      return isUpcoming(run)
    case 'LATE':
      return isLate(run)
    case 'DUE_TODAY':
      return isDueToday(run)
    case 'IN_PROGRESS':
      return run.status === 'IN_PROGRESS'
    case 'COMPLETED':
      return run.status === 'COMPLETED'
  }
}

const deduplicatedChecklistRuns = computed(() => selectLatestChecklistRuns(allChecklistRuns.value))

function triageSourceRuns(filter: TriageFilter): ChecklistRun[] {
  if (filter === 'UPCOMING' || filter === 'DUE_TODAY' || filter === 'LATE') {
    return allChecklistRuns.value
  }

  return deduplicatedChecklistRuns.value
}

const triageOptions = computed(() => {
  const options: Array<{ key: TriageFilter; label: string; count: number }> = [
    { key: 'UPCOMING', label: 'Upcoming', count: 0 },
    { key: 'LATE', label: 'Late', count: 0 },
    { key: 'DUE_TODAY', label: 'Due today', count: 0 },
    { key: 'IN_PROGRESS', label: 'In progress', count: 0 },
    { key: 'COMPLETED', label: 'Completed', count: 0 },
  ]

  return options.map((option) => ({
    ...option,
    count: triageSourceRuns(option.key).filter((run) => matchesFilter(run, option.key)).length,
  }))
})

const hasSearchQuery = computed(() => searchQuery.value.trim().length > 0)
const selectedChecklistRunId = computed(() => {
  const routeQueryValue = route.query.checklistRunId
  return typeof routeQueryValue === 'string' && routeQueryValue.length > 0 ? routeQueryValue : null
})
const visibleChecklistRunId = computed(() => selectedChecklistRunId.value ?? pinnedChecklistRunId.value)
const isFilterVisuallyActive = (filter: TriageFilter) =>
  !hasSearchQuery.value && activeFilter.value === filter

const filteredChecklistRuns = computed(() => {
  const normalizedQuery = searchQuery.value.trim().toLowerCase()
  const sourceRuns = triageSourceRuns(activeFilter.value)

  return sourceRuns.filter((run) => {
    const haystack = [run.title, run.description ?? '', run.status.replace(/_/g, ' ')]
      .join(' ')
      .toLowerCase()

    if (normalizedQuery) {
      return haystack.includes(normalizedQuery)
    }

    return matchesFilter(run, activeFilter.value) || visibleChecklistRunId.value === run.id
  })
})

function toLocalDateKey(value: string): string {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateGroupLabel(value: string): string {
  return new Intl.DateTimeFormat('nb-NO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value))
}

function diffInLocalDays(from: Date, to: Date): number {
  const fromMidnight = new Date(from.getFullYear(), from.getMonth(), from.getDate())
  const toMidnight = new Date(to.getFullYear(), to.getMonth(), to.getDate())
  const millisecondsPerDay = 24 * 60 * 60 * 1000
  return Math.floor((toMidnight.getTime() - fromMidnight.getTime()) / millisecondsPerDay)
}

function getDateGroupStyle(runs: ChecklistRun[], index: number) {
  const now = new Date()
  const maxOverdueDays = runs.reduce((currentMax, run) => {
    if (!isOverdue(run) && run.status !== 'OVERDUE') {
      return currentMax
    }

    return Math.max(currentMax, diffInLocalDays(new Date(run.dueAt), now))
  }, -1)

  if (maxOverdueDays >= 0) {
    if (maxOverdueDays >= 3) {
      return {
        '--date-group-background': '#efd6d3',
        '--date-group-border': '#bf7b72',
      }
    }

    if (maxOverdueDays >= 1) {
      return {
        '--date-group-background': '#f3e2d8',
        '--date-group-border': '#c89573',
      }
    }

    return {
      '--date-group-background': '#f6ede4',
      '--date-group-border': '#d4aa8b',
    }
  }

  switch (index % 4) {
    case 0:
      return {
        '--date-group-background': '#dcebe4',
        '--date-group-border': '#8fae9e',
      }
    case 1:
      return {
        '--date-group-background': '#dde8f6',
        '--date-group-border': '#8fa6c2',
      }
    case 2:
      return {
        '--date-group-background': '#e8e2f1',
        '--date-group-border': '#a598bb',
      }
    default:
      return {
        '--date-group-background': '#ddebef',
        '--date-group-border': '#8fa9b3',
      }
  }
}

const groupedChecklistRuns = computed(() => {
  const groups = new Map<
    string,
    {
      key: string
      label: string
      runs: ChecklistRun[]
    }
  >()

  for (const run of filteredChecklistRuns.value) {
    const key = toLocalDateKey(run.dueAt)
    const existing = groups.get(key)

    if (existing) {
      existing.runs.push(run)
      continue
    }

    groups.set(key, {
      key,
      label: formatDateGroupLabel(run.dueAt),
      runs: [run],
    })
  }

  return [...groups.values()]
})

watch(hasSearchQuery, (isSearching) => {
  if (isSearching) {
    pinnedChecklistRunId.value = null
  }
})

watch(allChecklistRuns, (runs) => {
  if (pinnedChecklistRunId.value && !runs.some((run) => run.id === pinnedChecklistRunId.value)) {
    pinnedChecklistRunId.value = null
  }
})

watch(
  resolvedChecklistContext,
  async (context) => {
    if (!context) {
      allChecklistRuns.value = []
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

    <template v-else>
      <ChecklistDefinitionManager
        v-if="canManageChecklistDefinitions && selectedManagementEstablishmentId"
        :organization-id="resolvedChecklistContext!.organizationId"
        :establishment-id="selectedManagementEstablishmentId"
        service-area="IK_MAT"
        :requested-definition-group-id="requestedDefinitionGroupId"
        @saved="handleDefinitionsSaved"
        @request-handled="clearDefinitionEditRequest"
      />

      <div
        v-else-if="canManageChecklistDefinitions"
        class="state-card"
      >
        <p>Choose a single establishment to create, edit, or delete scheduled checklist setups.</p>
      </div>

      <section v-if="allChecklistRuns.length > 0" class="triage-bar" aria-label="Checklist triage">
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

      <div v-if="allChecklistRuns.length > 0 && filteredChecklistRuns.length > 0" class="date-groups">
        <section
          v-for="(group, index) in groupedChecklistRuns"
          :key="group.key"
          class="date-group"
          :style="getDateGroupStyle(group.runs, index)"
        >
          <header class="date-group-header">
            <h2>{{ group.label }}</h2>
            <span>{{ group.runs.length }} {{ group.runs.length === 1 ? 'run' : 'runs' }}</span>
          </header>

          <div class="runs-grid">
            <ChecklistRunCard
              v-for="run in group.runs"
              :key="run.id"
              :run="run"
              :organization-id="resolvedChecklistContext!.organizationId"
              :establishment-id="resolvedChecklistContext!.establishmentIds[0] ?? ''"
              :selected="visibleChecklistRunId === run.id"
              :force-expanded="visibleChecklistRunId === run.id"
              :show-setup-actions="canManageChecklistDefinitions && selectedManagementEstablishmentId === run.establishmentId"
              :can-manage-assignments="canManageChecklistDefinitions"
              @update:run="handleRunUpdate"
              @edit:definition-group="handleDefinitionEditRequest"
            />
          </div>
        </section>
      </div>

      <div v-else-if="allChecklistRuns.length > 0" class="state-card">
        <p>{{ hasSearchQuery ? 'No checklist runs match your search.' : 'No checklist runs match the current filter.' }}</p>
      </div>

      <div v-else class="state-card">
        <p>
          {{
            canManageChecklistDefinitions && selectedManagementEstablishmentId
              ? 'No checklist runs yet. Create a one-off or recurring checklist setup above.'
              : 'No checklist runs found.'
          }}
        </p>
      </div>
    </template>
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
  padding: 0.1rem 0.45rem;
  border-radius: 0.5cqh;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.search-field {
  min-width: min(100%, 280px);
}

.search-field input {
  width: 100%;
  min-height: 2.75rem;
  padding: 0.75rem 0.875rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 0.5cqh;
  background: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
}

.search-field input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.runs-grid {
  display: grid;
  gap: 16px;
}

.date-groups {
  display: grid;
  gap: 18px;
}

.date-group {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--date-group-border, var(--color-border));
  border-radius: 0.5cqh;
  background: var(--date-group-background, var(--color-container));
}

.date-group:nth-child(4n + 1) {
  --date-group-background: color-mix(in srgb, var(--color-container) 72%, #f4efe2 28%);
  --date-group-border: color-mix(in srgb, var(--color-border) 64%, #d4b26a 36%);
}

.date-group:nth-child(4n + 2) {
  --date-group-background: color-mix(in srgb, var(--color-container) 70%, #e4efe8 30%);
  --date-group-border: color-mix(in srgb, var(--color-border) 62%, #7ea783 38%);
}

.date-group:nth-child(4n + 3) {
  --date-group-background: color-mix(in srgb, var(--color-container) 70%, #e9edf5 30%);
  --date-group-border: color-mix(in srgb, var(--color-border) 62%, #7d94bf 38%);
}

.date-group:nth-child(4n) {
  --date-group-background: color-mix(in srgb, var(--color-container) 70%, #f1e6e6 30%);
  --date-group-border: color-mix(in srgb, var(--color-border) 62%, #c49090 38%);
}

.date-group-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.date-group-header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1rem;
  font-weight: 700;
  text-transform: capitalize;
}

.date-group-header span {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.state-card {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 0.5cqh;
  background: var(--color-container);
}

.state-card-error {
  border-color: color-mix(in srgb, var(--color-critical) 35%, var(--color-border));
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
