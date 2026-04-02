<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createImportantDocuments } from '@/ik-alkohol/model/document.mock'
import {
  expiryWarningDays,
  formatDocumentStatus,
  getDocumentsWithStatus,
  parseLocalDate,
} from '@/ik-alkohol/model/document.utils'
import type {
  ImportantDocumentListItem,
  ImportantDocumentStatus,
} from '@/ik-alkohol/model/document.types'
import BaseButton from '@/shared/components/BaseButton.vue'

const router = useRouter()
const searchQuery = ref('')
const activeFilter = ref<'ALL' | ImportantDocumentStatus>('ALL')
const documents = createImportantDocuments()

const filterOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'VALID', label: 'Valid' },
  { value: 'EXPIRING', label: 'Expiring' },
  { value: 'EXPIRED', label: 'Expired' },
] as const

const documentsWithStatus = computed<ImportantDocumentListItem[]>(() => {
  return getDocumentsWithStatus(documents, expiryWarningDays)
})

const filteredDocuments = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  const items = documentsWithStatus.value.filter((documentRecord) => {
    if (!query) {
      return true
    }

    return [documentRecord.title, documentRecord.holderName]
      .join(' ')
      .toLowerCase()
      .includes(query)
  })

  if (activeFilter.value === 'ALL') {
    return items
  }

  return items.filter((documentRecord) => documentRecord.status === activeFilter.value)
})

const expiredCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRED').length
})

const expiringCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status === 'EXPIRING').length
})

const criticalCount = computed(() => expiredCount.value + expiringCount.value)

const readyForAuditCount = computed(() => {
  return documentsWithStatus.value.filter((documentRecord) => documentRecord.status !== 'EXPIRED').length
})

const readinessPercentage = computed(() => {
  if (documentsWithStatus.value.length === 0) {
    return 0
  }

  return Math.round((readyForAuditCount.value / documentsWithStatus.value.length) * 100)
})

const emptyStateMessage = computed(() => {
  if (searchQuery.value.trim()) {
    return 'No documents matched your search.'
  }

  if (activeFilter.value === 'VALID') {
    return 'No valid documents found.'
  }

  if (activeFilter.value === 'EXPIRING') {
    return 'No expiring documents found.'
  }

  if (activeFilter.value === 'EXPIRED') {
    return 'No expired documents found.'
  }

  return 'No documents registered yet.'
})

function formatDate(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
  }).format(parseLocalDate(value))
}

function goToUploadPage() {
  void router.push({ name: 'ik-alkohol-documents-upload' })
}
</script>

<template>
  <div class="documents-page">
    <header class="page-header">
      <div class="page-header-copy">
        <h1>Important documents</h1>
        <p class="page-subtitle">
          Track licences, staff records, and supporting alcohol-control documentation.
        </p>
      </div>

      <BaseButton class="upload-button" type="button" @click="goToUploadPage">
        Upload new document
      </BaseButton>
    </header>

    <section class="summary-grid" aria-label="Document overview">
      <article class="summary-card summary-card-critical">
        <p class="summary-label">Critical attention required</p>
        <p class="summary-value">
          {{ criticalCount }} {{ criticalCount === 1 ? 'document' : 'documents' }}
        </p>
        <p class="summary-support">
          {{ expiredCount }} expired • {{ expiringCount }} expiring within {{ expiryWarningDays }} days
        </p>
      </article>

      <article class="summary-card summary-card-readiness">
        <p class="summary-label">Audit readiness</p>
        <p class="summary-value">{{ readinessPercentage }}%</p>
        <div aria-hidden="true" class="readiness-bar">
          <span :style="{ width: `${readinessPercentage}%` }"></span>
        </div>
        <p class="summary-support">
          {{ readyForAuditCount }}/{{ documentsWithStatus.length }} documents ready for audit.
        </p>
      </article>
    </section>

    <section aria-label="Important documents overview" class="documents-panel">
      <div class="list-toolbar">
        <div class="search-field">
          <label class="search-label" for="document-search">Search</label>
          <input
            id="document-search"
            v-model="searchQuery"
            class="search-input"
            placeholder="Search documents"
            type="search"
          />
        </div>

        <div aria-label="Document filters" class="filter-group">
          <button
            v-for="filterOption in filterOptions"
            :key="filterOption.value"
            :data-active="activeFilter === filterOption.value"
            class="filter-chip"
            type="button"
            @click="activeFilter = filterOption.value"
          >
            {{ filterOption.label }}
          </button>
        </div>
      </div>

      <table v-if="filteredDocuments.length > 0" class="documents-table">
        <thead class="documents-table-header">
          <tr>
            <th scope="col">Document title</th>
            <th scope="col">Staff/entity</th>
            <th scope="col">Issue date</th>
            <th scope="col">Renewal date</th>
            <th scope="col">Status</th>
            <th scope="col" class="actions-column">Actions</th>
          </tr>
        </thead>

        <tbody class="documents-list">
          <tr v-for="documentRecord in filteredDocuments" :key="documentRecord.id" class="documents-list-item">
            <th scope="row" class="document-cell document-cell-title">
              <span class="document-cell-label">Document title</span>
              <span class="document-primary">{{ documentRecord.title }}</span>
            </th>

            <td class="document-cell">
              <span class="document-cell-label">Staff/entity</span>
              <span>{{ documentRecord.holderName }}</span>
            </td>

            <td class="document-cell">
              <span class="document-cell-label">Issue date</span>
              <span>{{ formatDate(documentRecord.issueDate) }}</span>
            </td>

            <td class="document-cell">
              <span class="document-cell-label">Renewal date</span>
              <span>{{ formatDate(documentRecord.renewalDate) }}</span>
            </td>

            <td class="document-cell">
              <span class="document-cell-label">Status</span>
              <span class="status-badge" :data-status="documentRecord.status">
                {{ formatDocumentStatus(documentRecord.status) }}
              </span>
            </td>

            <td class="document-cell document-cell-actions">
              <span class="document-cell-label">Actions</span>
              <button
                type="button"
                class="action-button"
                :aria-label="`Document actions for ${documentRecord.title}`"
              >
                <svg aria-hidden="true" class="action-icon" viewBox="0 0 20 20">
                  <circle cx="10" cy="4.5" r="1.5" fill="currentColor" />
                  <circle cx="10" cy="10" r="1.5" fill="currentColor" />
                  <circle cx="10" cy="15.5" r="1.5" fill="currentColor" />
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <p>{{ emptyStateMessage }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.documents-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header-copy h1,
.page-subtitle,
.summary-label,
.summary-value,
.summary-support,
.document-primary,
.document-cell p,
.document-cell-label,
.empty-state p {
  margin: 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  max-width: 72ch;
}

.upload-button {
  width: auto;
  min-width: 0;
  flex-shrink: 0;
  font-weight: 600;
}

.upload-button :deep(.button) {
  width: auto;
  justify-content: center;
  padding: 0.8125rem 0.875rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
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

.summary-label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.summary-value {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.summary-support {
  font-size: 0.875rem;
}

.readiness-bar {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background-color: rgba(29, 78, 216, 0.14);
  overflow: hidden;
}

.readiness-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background-color: #2563eb;
}

.documents-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid var(--color-border-muted);
}

.search-field {
  display: flex;
  min-width: min(100%, 320px);
  flex: 1 1 320px;
  flex-direction: column;
  gap: 6px;
}

.search-label,
.document-cell-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.search-input {
  width: 100%;
  padding: 0.875rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.search-input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-color: transparent;
}

.filter-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: 0.875rem;
  cursor: pointer;
}

.filter-chip[data-active='true'] {
  border-color: var(--color-text-primary);
  color: var(--color-text-primary);
}

.filter-chip:hover {
  color: var(--color-text-primary);
}

.documents-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.documents-table-header tr,
.documents-list-item {
  display: grid;
  grid-template-columns: minmax(0, 2.1fr) minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr) minmax(0, 0.9fr) 48px;
  column-gap: 16px;
}

.documents-table-header {
  background-color: #e2e8f0;
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.documents-table-header th {
  padding: 14px 20px;
  font: inherit;
  text-align: left;
}

.actions-column {
  text-align: center;
}

.documents-list-item + .documents-list-item {
  border-top: 1px solid #e2e8f0;
}

.documents-list-item {
  align-items: center;
  background-color: var(--color-container);
  transition: background-color 120ms ease;
}

.documents-list-item:hover {
  background-color: #f8fafc;
}

.document-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 18px 20px;
  text-align: left;
}

.document-cell-label {
  display: none;
}

.document-primary {
  min-width: 0;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.document-cell span:not(.document-cell-label):not(.status-badge) {
  min-width: 0;
  overflow-wrap: anywhere;
}

.status-badge {
  display: inline-flex;
  align-self: flex-start;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  background-color: var(--color-surface);
  font-size: 0.75rem;
  font-weight: 600;
}

.status-badge[data-status='VALID'] {
  background-color: #dbeafe;
  color: #1d4ed8;
}

.status-badge[data-status='EXPIRING'] {
  background-color: #e0e7ff;
  color: #4c51bf;
}

.status-badge[data-status='EXPIRED'] {
  background-color: #fecaca;
  color: #991b1b;
}

.document-cell-actions {
  align-items: center;
  justify-content: center;
}

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 4px;
  background-color: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.action-button:hover {
  background-color: var(--color-surface);
  color: var(--color-text-primary);
}

.action-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.empty-state {
  padding: 24px;
  color: var(--color-text-secondary);
}

@media (max-width: 1080px) {
  .documents-table-header {
    display: none;
  }

  .documents-list-item {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 12px;
  }

  .document-cell-label {
    display: block;
  }

  .document-cell-title,
  .document-cell-actions {
    grid-column: 1 / -1;
  }

  .document-cell-actions {
    align-items: flex-start;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }

  .upload-button {
    align-self: flex-start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .documents-list-item {
    grid-template-columns: 1fr;
  }
}
</style>
