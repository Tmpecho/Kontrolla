<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import ChecklistRunCard from '@/checklists/components/ChecklistRunCard.vue'
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

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

function handleRunUpdate(updatedRun: ChecklistRun) {
  checklistRuns.value = checklistRuns.value.map((run) =>
    run.id === updatedRun.id ? updatedRun : run,
  )
}

onMounted(async () => {
  await loadChecklistRuns()
})
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

    <div v-else-if="checklistRuns.length > 0" class="runs-grid">
      <ChecklistRunCard
        v-for="run in checklistRuns"
        :key="run.id"
        :run="run"
        :organization-id="appEnv.defaultOrganizationId!"
        :establishment-id="appEnv.defaultEstablishmentId!"
        @update:run="handleRunUpdate"
      />
    </div>

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
</style>
