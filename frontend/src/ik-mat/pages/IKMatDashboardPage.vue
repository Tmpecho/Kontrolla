<script setup lang="ts">
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
  <section>
    <h1>IK-mat Dashboard</h1>
    <p>Overview over food compliance.</p>
  </section>

  <section>
    <h2>Checklist runs</h2>

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
  </section>
</template>
