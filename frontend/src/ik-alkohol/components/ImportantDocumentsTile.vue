<script setup lang="ts">
import { computed } from 'vue'

import { createImportantDocuments } from '@/ik-alkohol/model/document.mock'
import { expiryWarningDays, getDocumentsWithStatus } from '@/ik-alkohol/model/document.utils'

const documents = createImportantDocuments()

const documentsWithStatus = computed(() => getDocumentsWithStatus(documents, expiryWarningDays))

const expiredCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRED').length
})

const expiringCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRING').length
})

const validatedCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status !== 'EXPIRED').length
})

const readinessPercentage = computed(() => {
  if (documentsWithStatus.value.length === 0) {
    return 0
  }

  return Math.round((validatedCount.value / documentsWithStatus.value.length) * 100)
})

const nextRenewalDocument = computed(() => documentsWithStatus.value[0] ?? null)

function formatDate(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
  }).format(new Date(value))
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

    <div class="summary-grid">
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
          {{ validatedCount }}/{{ documentsWithStatus.length }} documents currently valid
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
.summary-label,
.summary-value,
.summary-hint,
.next-renewal-label,
.next-renewal-title,
.next-renewal-meta {
  margin: 0;
}

.tile-subtitle,
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
