<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import {
  deleteDocument,
  downloadDocumentFile,
  listAllEstablishmentDocuments,
} from '@/documents/api/documents.api'
import type {
  DocumentServiceArea,
  DocumentStatus,
  EstablishmentDocument,
} from '@/documents/model/document.types'
import {
  expiryWarningDays,
  formatDocumentStatus,
  parseLocalDate,
  sortDocumentsByRenewalDate,
} from '@/documents/model/document.utils'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'
import { appEnv } from '@/shared/config/env'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const searchQuery = ref('')
const activeFilter = ref<'ALL' | DocumentStatus>('ALL')
const documents = ref<EstablishmentDocument[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const actionErrorMessage = ref<string | null>(null)
const activeDownloadDocumentId = ref<string | null>(null)
const activeDeleteDocumentId = ref<string | null>(null)

const filterOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'VALID', label: 'Valid' },
  { value: 'EXPIRING', label: 'Expiring' },
  { value: 'EXPIRED', label: 'Expired' },
] as const

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(
  () => authStore.appContext?.establishmentId ?? appEnv.defaultEstablishmentId ?? null,
)

const hasDocumentContext = computed(() => Boolean(organizationId.value && establishmentId.value))

const missingContextMessage = computed(() => {
  if (hasDocumentContext.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Documents cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID or sign in with an organization context to load documents.'
})

const currentServiceArea = computed<DocumentServiceArea>(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    return 'IK_ALKOHOL'
  }

  return 'IK_MAT'
})

const isAlcoholPage = computed(() => currentServiceArea.value === 'IK_ALKOHOL')

const pageTitle = computed(() => {
  if (isAlcoholPage.value) {
    return 'Important documents'
  }

  return 'Documents'
})

const pageSubtitle = computed(() => {
  if (isAlcoholPage.value) {
    return 'Track licences, staff records, and supporting alcohol-control documentation.'
  }

  return 'Track certificates, routines, and supporting food-safety documentation.'
})

const panelLabel = computed(() => {
  if (isAlcoholPage.value) {
    return 'Important documents overview'
  }

  return 'Documents overview'
})

const uploadRouteName = computed(() => {
  if (isAlcoholPage.value) {
    return 'ik-alkohol-documents-upload'
  }

  return 'ik-mat-documents-upload'
})

const canManageDocuments = computed(() => {
  if (authStore.user?.globalRoles?.includes('PLATFORM_ADMIN')) {
    return true
  }

  const organizationRole = authStore.appContext?.organizationRole
  return (
    organizationRole === 'ORG_OWNER' ||
    organizationRole === 'ORG_ADMIN' ||
    organizationRole === 'ORG_MANAGER'
  )
})

const documentsWithStatus = computed(() => sortDocumentsByRenewalDate(documents.value))

const filteredDocuments = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  const items = documentsWithStatus.value.filter((documentRecord) => {
    if (!query) {
      return true
    }

    return [documentRecord.title, documentRecord.holderName, documentRecord.fileName]
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

const showSummary = computed(() => {
  return !missingContextMessage.value && !isLoading.value && !errorMessage.value
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

let requestSequence = 0

watch(currentServiceArea, () => {
  searchQuery.value = ''
  activeFilter.value = 'ALL'
})

watch([organizationId, establishmentId, currentServiceArea], () => {
  void loadDocuments()
}, { immediate: true })

function formatDate(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
  }).format(parseLocalDate(value))
}

function goToUploadPage() {
  void router.push({ name: uploadRouteName.value })
}

function isDownloadingDocument(documentId: string) {
  return activeDownloadDocumentId.value === documentId
}

function isDeletingDocument(documentId: string) {
  return activeDeleteDocumentId.value === documentId
}

async function handleDownloadDocument(documentRecord: EstablishmentDocument): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    return
  }

  actionErrorMessage.value = null
  activeDownloadDocumentId.value = documentRecord.id

  try {
    const file = await downloadDocumentFile({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      documentId: documentRecord.id,
    })

    triggerBrowserDownload(file.blob, file.fileName)
  } catch (error) {
    actionErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to download document.'
  } finally {
    if (activeDownloadDocumentId.value === documentRecord.id) {
      activeDownloadDocumentId.value = null
    }
  }
}

async function handleDeleteDocument(documentRecord: EstablishmentDocument): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !canManageDocuments.value) {
    return
  }

  if (!window.confirm(`Delete "${documentRecord.title}"? This cannot be undone.`)) {
    return
  }

  actionErrorMessage.value = null
  activeDeleteDocumentId.value = documentRecord.id

  try {
    await deleteDocument({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      documentId: documentRecord.id,
    })

    documents.value = documents.value.filter(
      (existingDocument) => existingDocument.id !== documentRecord.id,
    )
  } catch (error) {
    actionErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to delete document.'
  } finally {
    if (activeDeleteDocumentId.value === documentRecord.id) {
      activeDeleteDocumentId.value = null
    }
  }
}

function triggerBrowserDownload(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  link.click()
  URL.revokeObjectURL(objectUrl)
}

async function loadDocuments(): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value
  const serviceArea = currentServiceArea.value
  const requestId = ++requestSequence

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    documents.value = []
    errorMessage.value = null
    actionErrorMessage.value = null
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = null
  actionErrorMessage.value = null

  try {
    const allDocuments = await listAllEstablishmentDocuments({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      serviceArea,
      size: 100,
    })

    if (requestId !== requestSequence) {
      return
    }

    documents.value = sortDocumentsByRenewalDate(allDocuments)
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
  <div class="documents-page">
    <header class="page-header">
      <div class="page-header-copy">
        <h1>{{ pageTitle }}</h1>
        <p class="page-subtitle">{{ pageSubtitle }}</p>
      </div>

      <BaseButton
        v-if="canManageDocuments"
        class="upload-button"
        type="button"
        @click="goToUploadPage"
      >
        Upload new document
      </BaseButton>
    </header>

    <section v-if="showSummary" class="summary-grid" aria-label="Document overview">
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

    <section :aria-label="panelLabel" class="documents-panel">
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

      <div v-if="missingContextMessage" class="empty-state">
        <p>{{ missingContextMessage }}</p>
      </div>

      <div v-else-if="isLoading" class="empty-state">
        <p>Loading documents...</p>
      </div>

      <div v-else-if="errorMessage" class="empty-state">
        <p>{{ errorMessage }}</p>
      </div>

      <div v-if="!missingContextMessage && !isLoading && !errorMessage && actionErrorMessage" class="action-feedback" role="alert">
        <p>{{ actionErrorMessage }}</p>
      </div>

      <table
        v-if="!missingContextMessage && !isLoading && !errorMessage && filteredDocuments.length > 0"
        class="documents-table"
      >
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
              <div class="document-actions">
                <button
                  type="button"
                  class="document-action-button document-action-button-download"
                  :disabled="isDownloadingDocument(documentRecord.id) || isDeletingDocument(documentRecord.id)"
                  @click="handleDownloadDocument(documentRecord)"
                >
                  {{ isDownloadingDocument(documentRecord.id) ? 'Downloading...' : 'Download' }}
                </button>

                <button
                  v-if="canManageDocuments"
                  type="button"
                  class="document-action-button document-action-button-delete"
                  :disabled="isDeletingDocument(documentRecord.id) || isDownloadingDocument(documentRecord.id)"
                  @click="handleDeleteDocument(documentRecord)"
                >
                  {{ isDeletingDocument(documentRecord.id) ? 'Deleting...' : 'Delete' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!missingContextMessage && !isLoading && !errorMessage && filteredDocuments.length === 0" class="empty-state">
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
  grid-template-columns:
    minmax(0, 2.1fr)
    minmax(0, 1.3fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 0.9fr)
    minmax(176px, 1.2fr);
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
  padding-left: 12px;
  padding-right: 12px;
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
  padding-left: 12px;
  padding-right: 12px;
}

.document-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.document-action-button {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-primary);
  font: inherit;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.document-action-button:hover:not(:disabled) {
  border-color: var(--color-text-primary);
}

.document-action-button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.document-action-button-delete {
  border-color: #fecaca;
  color: #b91c1c;
}

.document-action-button-delete:hover:not(:disabled) {
  border-color: #ef4444;
}

.action-feedback {
  padding: 16px 20px 0;
  color: #b91c1c;
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
