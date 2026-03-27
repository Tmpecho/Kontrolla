<script setup lang="ts">
import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'
import { computed, onMounted, ref } from 'vue'
import ChecklistRunCard from '@/checklists/components/ChecklistRunCard.vue'

const checklistRuns = ref<ChecklistRun[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

const hasChecklistContext = computed(() =>
  Boolean(appEnv.defaultOrganizationId && appEnv.defaultEstablishmentId),
)

const missingContextMessage = computed(() => {
  if (hasChecklistContext.value) return null
  if (!appEnv.isDevelopment) {
    return 'Checklist runs cannot be loaded until organization and establishment context is available.'
  }
  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID to load checklist runs.'
})

async function loadChecklistRuns(): Promise<void> {
  const organizationId = appEnv.defaultOrganizationId
  const establishmentId = appEnv.defaultEstablishmentId

  if (!organizationId || !establishmentId) return

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
  <div class="page-container">
    <header class="page-header">
      <h1>IK-mat Dashboard</h1>
      <p>Overview of food safety compliance routines.</p>
    </header>

    <!-- Loading & Error States -->
    <div v-if="missingContextMessage" class="state-card">
      <p>{{ missingContextMessage }}</p>
    </div>

    <div v-else-if="isLoading" class="state-card loading">
      <div class="spinner" />
      <p>Loading checklist runs...</p>
    </div>

    <div v-else-if="errorMessage" class="state-card error">
      <p>{{ errorMessage }}</p>
    </div>

    <!-- Main Content -->
    <div v-else-if="checklistRuns.length > 0" class="runs-grid">
      <ChecklistRunCard v-for="run in checklistRuns" :key="run.id" :run="run" />
    </div>

    <div v-else class="state-card">
      <p>No checklist runs found.</p>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 60rem;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.page-header {
  margin-bottom: 2.5rem;
}

.page-header h1 {
  font-size: 1.875rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem;
  letter-spacing: -0.02em;
}

.page-header p {
  font-size: 1rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.runs-grid {
  display: grid;
  gap: 1.5rem;
}

.state-card {
  background: var(--color-container);
  padding: 2rem;
  border-radius: 0.75rem;
  text-align: center;
  color: var(--color-text-secondary);
  box-shadow: var(--shadow-elevated);
}

.state-card.error {
  border: 1px solid var(--color-critical);
  color: var(--color-critical);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--color-border-muted);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
