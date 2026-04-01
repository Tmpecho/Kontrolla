<script setup lang="ts">
import DeviationsTile from '@/shared/components/DeviationsTile.vue'
import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'
import { computed, onMounted, ref } from 'vue'

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

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatStatus(status: ChecklistRun['status']): string {
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function formatTaskExecutionStatus(status: ChecklistRun['tasks'][number]['executionStatus']): string {
  return status === 'COMPLETED' ? 'Completed' : status === 'SKIPPED' ? 'Skipped' : 'Pending'
}

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

    checklistRuns.value = page.items
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
      <div class="dashboard-tile">
        <h2>Checklist</h2>

        <p v-if="missingContextMessage">{{ missingContextMessage }}</p>
        <p v-else-if="isLoading">Loading checklist runs...</p>
        <p v-else-if="errorMessage">{{ errorMessage }}</p>
        <p v-else-if="checklistRuns.length === 0">No checklist runs found.</p>

        <ul v-else style="padding-left: 1.25rem">
          <li v-for="run in checklistRuns" :key="run.id" style="margin-bottom: 0.75rem">
            <strong>{{ run.title }}</strong>
            <div>Due: {{ formatDateTime(run.dueAt) }}</div>
            <div>Status: {{ formatStatus(run.status) }}</div>
            <div>Assignments: {{ run.assignments.length }}</div>

            <div v-if="run.tasks.length > 0" style="margin-top: 0.5rem">
              <div>Tasks:</div>
              <ul style="padding-left: 1.25rem; margin-top: 0.25rem">
                <li v-for="task in run.tasks" :key="task.checklistTaskExecutionId">
                  {{ task.title }} - {{ formatTaskExecutionStatus(task.executionStatus) }}
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <section class="dashboard-section">
      <DeviationsTile class="dashboard-tile" to="/app/ik-mat/deviation/form" />
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
  flex-direction: column;
  gap: 12px;
  flex: 1;
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

.row-container {
  display: flex;
  flex-direction: row;
  gap: 24px
}

</style>
