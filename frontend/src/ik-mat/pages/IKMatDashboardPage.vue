<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import { selectLatestChecklistRuns } from '@/checklists/model/checklist-runs.utils'
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'
import DeviationsTile from '@/shared/components/DeviationsTile.vue'

const checklistRuns = ref<ChecklistRun[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

const hasChecklistContext = computed(
  () => Boolean(appEnv.defaultOrganizationId && appEnv.defaultEstablishmentId),
)

const missingContextMessage = computed(() => {
  if (hasChecklistContext.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Checklist runs cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID to load checklist runs in development.'
})

const activeRunsCount = computed(() => {
  return checklistRuns.value.filter((run) => !['COMPLETED', 'CANCELLED'].includes(run.status)).length
})

const overdueRunsCount = computed(() => {
  return checklistRuns.value.filter((run) => run.status === 'OVERDUE').length
})

const inProgressRunsCount = computed(() => {
  return checklistRuns.value.filter((run) => run.status === 'IN_PROGRESS').length
})

const activeRunsLabel = computed(() => {
  return `${activeRunsCount.value} active ${activeRunsCount.value === 1 ? 'run' : 'runs'}`
})

async function loadChecklistRuns(): Promise<void> {
  const organizationId = appEnv.defaultOrganizationId
  const establishmentId = appEnv.defaultEstablishmentId

  if (!organizationId || !establishmentId) {
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const page = await listChecklistRuns({
      organizationId,
      establishmentId,
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

onMounted(async () => {
  await loadChecklistRuns()
})
</script>

<template>
  <div class="dashboard-page">
    <section>
      <h1>IK-mat Dashboard</h1>
      <p>Overview over food compliance.</p>
    </section>

    <section class="dashboard-section">
      <RouterLink :to="{ name: 'ik-mat-checklists' }" class="tile-link">
        <div class="dashboard-tile dashboard-tile-link">
          <div class="tile-copy">
            <h2>Checklists</h2>
            <p>Open scheduled routines and update task progress.</p>
          </div>

          <p v-if="missingContextMessage" class="tile-meta">{{ missingContextMessage }}</p>
          <p v-else-if="isLoading" class="tile-meta">Loading checklist runs...</p>
          <p v-else-if="errorMessage" class="tile-meta tile-meta-error">{{ errorMessage }}</p>
          <p v-else-if="checklistRuns.length === 0" class="tile-meta">No checklist runs found.</p>
          <div v-else class="checklist-summary">
            <p class="summary-primary">{{ activeRunsLabel }}</p>
            <p class="summary-secondary">
              {{ overdueRunsCount }} overdue • {{ inProgressRunsCount }} in progress
            </p>
          </div>
        </div>
      </RouterLink>
    </section>

    <section class="dashboard-section">
      <DeviationsTile
        add-deviation-to="/app/ik-mat/deviation/form"
        class="dashboard-tile"
        deviation-page-to="/app/ik-mat/deviation"
        service-area="IK_MAT"
      />
    </section>

    <div class="row-container">
      <section class="dashboard-section">
        <RouterLink :to="{ name: 'ik-mat-documents' }" class="tile-link">
          <div class="dashboard-tile dashboard-tile-link">
            <h2>Documents</h2>
            <p>Overview over IK-mat related documents.</p>
          </div>
        </RouterLink>
      </section>

      <section class="dashboard-section">
        <div class="dashboard-tile dashboard-tile-link">
          <h2>Temperature</h2>
          <p>Temperature logs ...</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-section {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.dashboard-tile {
  display: flex;
  min-height: 120px;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  text-decoration: none;
}

.tile-link {
  text-decoration: none;
}

.dashboard-tile-link:hover {
  border-color: var(--color-primary);
}

.tile-copy,
.checklist-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tile-copy h2,
.tile-copy p,
.summary-primary,
.summary-secondary,
.tile-meta {
  margin: 0;
}

.tile-copy p,
.summary-secondary,
.tile-meta {
  color: var(--color-text-secondary);
}

.summary-primary {
  font-weight: 600;
}

.tile-meta-error {
  color: var(--color-critical);
}

.row-container {
  display: flex;
  flex-direction: row;
  gap: 24px;
}

@media (max-width: 960px) {
  .row-container {
    flex-direction: column;
  }
}
</style>
