<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import { listEstablishmentDocuments } from '@/documents/api/documents.api'
import type { EstablishmentDocument } from '@/documents/model/document.types'
import {
  expiryWarningDays,
  parseLocalDate,
  sortDocumentsByRenewalDate,
} from '@/documents/model/document.utils'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

const authStore = useAuthStore()
const documents = ref<EstablishmentDocument[]>([])
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

  if (!appEnv.isDevelopment) {
    return 'Documents are unavailable until organization context is ready.'
  }

  return 'Set the default organization and establishment IDs or sign in with an organization context to load documents.'
})

const documentsWithStatus = computed(() => sortDocumentsByRenewalDate(documents.value))

const expiredCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRED').length
})

const expiringCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRING').length
})

const readyCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status !== 'EXPIRED').length
})

const readinessPercentage = computed(() => {
  if (documentsWithStatus.value.length === 0) {
    return 0
  }

  return Math.round((readyCount.value / documentsWithStatus.value.length) * 100)
})

const nextRenewalDocument = computed(() => documentsWithStatus.value[0] ?? null)

let requestSequence = 0

watch([organizationId, establishmentId], () => {
  void loadDocuments()
}, { immediate: true })

function formatDate(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
  }).format(parseLocalDate(value))
}

async function loadDocuments(): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value
  const requestId = ++requestSequence

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    documents.value = []
    errorMessage.value = null
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const page = await listEstablishmentDocuments({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })

    if (requestId !== requestSequence) {
      return
    }

    documents.value = sortDocumentsByRenewalDate(page.items)
  } catch (error) {
    if (requestId !== requestSequence) {
      return
    }

    documents.value = []
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load documents.'
  } finally {
    if (requestId === requestSequence) {
      isLoading.value = false
    }
  }
}
</script>

<template>
  <div class="important-documents-tile">
    <div class="tile-header">
      <div>
        <h2>Important documents</h2>
        <p class="tile-subtitle">Licences, training records, and key alcohol-control documents.</p>
      </div>
      <RouterLink :to="{ name: 'ik-alkohol-documents' }" class="tile-link">Open</RouterLink>
    </div>

    <p v-if="missingContextMessage" class="tile-message">{{ missingContextMessage }}</p>
    <p v-else-if="isLoading" class="tile-message">Loading documents...</p>
    <p v-else-if="errorMessage" class="tile-message">{{ errorMessage }}</p>
    <p v-else-if="documentsWithStatus.length === 0" class="tile-message">No documents found.</p>

    <div v-else class="summary-grid">
      <div class="summary-card summary-card-critical">
        <p class="summary-label">Needs attention</p>
        <p class="summary-value">{{ expiredCount + expiringCount }}</p>
        <p class="summary-hint">
          {{ expiredCount }} expired • {{ expiringCount }} expiring within {{ expiryWarningDays }} days
        </p>
      </div>

      <div class="summary-card summary-card-readiness">
        <p class="summary-label">Audit readiness</p>
        <p class="summary-value">{{ readinessPercentage }}%</p>
        <p class="summary-hint">
          {{ readyCount }}/{{ documentsWithStatus.length }} documents ready for audit
        </p>
      </div>
    </div>

    <div v-if="nextRenewalDocument" class="next-renewal">
      <p class="next-renewal-label">Next renewal</p>
      <p class="next-renewal-title">{{ nextRenewalDocument.title }}</p>
      <p class="next-renewal-meta">
        {{ nextRenewalDocument.holderName }} • {{ formatDate(nextRenewalDocument.renewalDate) }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.important-documents-tile {
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
.tile-message,
.summary-label,
.summary-value,
.summary-hint,
.next-renewal-label,
.next-renewal-title,
.next-renewal-meta {
  margin: 0;
}

.tile-subtitle,
.tile-message,
.summary-hint,
.next-renewal-meta {
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

.tile-message {
  margin: 0;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 4px;
  border: 1px solid transparent;
}

.summary-card-critical {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}

.summary-card-readiness {
  background-color: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.summary-label,
.next-renewal-label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.summary-value {
  font-size: 1.375rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.next-renewal {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
}

.next-renewal-title {
  font-weight: 600;
}

@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
