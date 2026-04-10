<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { listOrganizationMembers } from '@/account/api/organization-members.api'
import type { OrganizationMembership } from '@/account/model/organization-members.types'
import { useProtectedWorkspaceContext } from '@/auth/model/workspace-context'
import { createDocument } from '@/documents/api/documents.api'
import type { DocumentServiceArea } from '@/documents/model/document.types'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'

const workspaceContext = useProtectedWorkspaceContext()
const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  holderName: '',
  issueDate: '',
  renewalDate: '',
})

const selectedFile = ref<File | null>(null)
const auditMembers = ref<OrganizationMembership[]>([])
const selectedAuditUserIds = ref<string[]>([])
const errorMessage = ref<string | null>(null)
const auditMembersErrorMessage = ref<string | null>(null)
const isSubmitting = ref(false)
const attemptedSubmit = ref(false)
const isLoadingAuditMembers = ref(false)

const isAlcoholPage = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return routeName.startsWith('ik-alkohol-')
})

const organizationId = workspaceContext.organizationId
const establishmentId = workspaceContext.establishmentId

const currentServiceArea = computed<DocumentServiceArea>(() => {
  if (isAlcoholPage.value) {
    return 'IK_ALKOHOL'
  }

  return 'IK_MAT'
})

const pageSubtitle = computed(() => {
  if (isAlcoholPage.value) {
    return 'Upload a PDF and register the metadata required for alcohol-control documentation.'
  }

  return 'Upload a PDF and register the metadata required for food-safety documentation.'
})

const missingContextMessage = computed(() => {
  if (workspaceContext.isStartupPending.value) {
    return null
  }

  if (workspaceContext.hasEstablishmentContext.value) {
    return null
  }

  if (workspaceContext.requiresEstablishmentSelection.value) {
    return 'Choose an establishment before uploading documents.'
  }

  return 'Documents cannot be uploaded until organization and establishment context is available.'
})

const backRouteName = computed(() => {
  if (isAlcoholPage.value) {
    return 'ik-alkohol-documents'
  }

  return 'ik-mat-documents'
})

const backLinkLabel = computed(() => {
  if (isAlcoholPage.value) {
    return 'Back to important documents'
  }

  return 'Back to documents'
})

const allAuditMembersSelected = computed(() => {
  return auditMembers.value.length > 0 && selectedAuditUserIds.value.length === auditMembers.value.length
})

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
}

const titleError = computed(() =>
  attemptedSubmit.value && !form.title.trim() ? 'Enter a document title.' : null,
)
const holderNameError = computed(() =>
  attemptedSubmit.value && !form.holderName.trim() ? 'Enter the holder name.' : null,
)
const issueDateError = computed(() =>
  attemptedSubmit.value && !form.issueDate ? 'Choose an issue date.' : null,
)
const renewalDateError = computed(() =>
  attemptedSubmit.value && !form.renewalDate ? 'Choose a renewal date.' : null,
)
const fileError = computed(() => {
  if (!attemptedSubmit.value) {
    return null
  }

  if (!selectedFile.value) {
    return 'Choose a PDF file to upload.'
  }

  const fileName = selectedFile.value.name.toLowerCase()
  const contentType = selectedFile.value.type.toLowerCase()

  if (contentType !== 'application/pdf' && !fileName.endsWith('.pdf')) {
    return 'Only PDF files are supported.'
  }

  return null
})

function formatMemberName(member: OrganizationMembership): string {
  return `${member.userFirstName} ${member.userLastName}`.trim() || member.userEmail
}

function toggleSelectAllAuditMembers() {
  if (allAuditMembersSelected.value) {
    selectedAuditUserIds.value = []
    return
  }

  selectedAuditUserIds.value = auditMembers.value.map((member) => member.userId)
}

async function loadAuditMembers() {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    auditMembers.value = []
    selectedAuditUserIds.value = []
    auditMembersErrorMessage.value = null
    return
  }

  isLoadingAuditMembers.value = true
  auditMembersErrorMessage.value = null

  try {
    const page = await listOrganizationMembers({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      includeInactive: false,
      size: 100,
    })

    auditMembers.value = page.items
    const validUserIds = new Set(page.items.map((member) => member.userId))
    selectedAuditUserIds.value = selectedAuditUserIds.value.filter((userId) => validUserIds.has(userId))
  } catch (error) {
    auditMembers.value = []
    selectedAuditUserIds.value = []
    auditMembersErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load audit readers.'
  } finally {
    isLoadingAuditMembers.value = false
  }
}

function clearFieldFeedback(): void {
  errorMessage.value = null
}

async function submitForm() {
  if (isSubmitting.value) {
    return
  }

  attemptedSubmit.value = true

  if (
    titleError.value ||
    holderNameError.value ||
    issueDateError.value ||
    renewalDateError.value ||
    fileError.value
  ) {
    return
  }

  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value
  const file = selectedFile.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !file) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    await createDocument({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      serviceArea: currentServiceArea.value,
      title: form.title.trim(),
      holderName: form.holderName.trim(),
      issueDate: form.issueDate,
      renewalDate: form.renewalDate,
      auditUserIds: selectedAuditUserIds.value,
      file,
    })

    await router.push({ name: backRouteName.value })
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to upload document.'
  } finally {
    isSubmitting.value = false
  }
}

watch([organizationId, establishmentId], () => {
  void loadAuditMembers()
}, { immediate: true })
</script>

<template>
  <div class="upload-page app-page">
    <header class="page-header app-page-header">
      <div class="app-page-header-copy">
        <h1 class="app-page-title">Upload new document</h1>
        <p class="page-subtitle app-page-subtitle">{{ pageSubtitle }}</p>
      </div>
    </header>

    <section v-if="missingContextMessage" class="placeholder-panel app-panel">
      <h2>Upload unavailable</h2>
      <p>{{ missingContextMessage }}</p>

      <RouterLink :to="{ name: backRouteName }" class="back-link">
        {{ backLinkLabel }}
      </RouterLink>
    </section>

    <form v-else class="upload-form app-panel" @submit.prevent="submitForm">
      <div class="form-grid">
        <label class="field">
          <span class="field-label">Document title</span>
          <input
            v-model="form.title"
            class="field-input"
            :class="{ 'field-input-error': Boolean(titleError) }"
            :aria-invalid="Boolean(titleError)"
            type="text"
            maxlength="255"
            @input="clearFieldFeedback"
          />
          <span v-if="titleError" class="field-error">{{ titleError }}</span>
        </label>

        <label class="field">
          <span class="field-label">Holder name</span>
          <input
            v-model="form.holderName"
            class="field-input"
            :class="{ 'field-input-error': Boolean(holderNameError) }"
            :aria-invalid="Boolean(holderNameError)"
            type="text"
            maxlength="255"
            @input="clearFieldFeedback"
          />
          <span v-if="holderNameError" class="field-error">{{ holderNameError }}</span>
        </label>

        <label class="field">
          <span class="field-label">Issue date</span>
          <input
            v-model="form.issueDate"
            class="field-input"
            :class="{ 'field-input-error': Boolean(issueDateError) }"
            :aria-invalid="Boolean(issueDateError)"
            type="date"
            @input="clearFieldFeedback"
          />
          <span v-if="issueDateError" class="field-error">{{ issueDateError }}</span>
        </label>

        <label class="field">
          <span class="field-label">Renewal date</span>
          <input
            v-model="form.renewalDate"
            class="field-input"
            :class="{ 'field-input-error': Boolean(renewalDateError) }"
            :aria-invalid="Boolean(renewalDateError)"
            type="date"
            @input="clearFieldFeedback"
          />
          <span v-if="renewalDateError" class="field-error">{{ renewalDateError }}</span>
        </label>

        <label class="field field-file">
          <span class="field-label">PDF file</span>
          <input
            class="field-input field-input-file"
            :class="{ 'field-input-error': Boolean(fileError) }"
            :aria-invalid="Boolean(fileError)"
            type="file"
            accept="application/pdf,.pdf"
            @change="onFileChange"
          />
          <span v-if="fileError" class="field-error">{{ fileError }}</span>
          <span class="field-help">
            {{ selectedFile ? selectedFile.name : 'Select a PDF file to upload.' }}
          </span>
        </label>

        <fieldset class="field field-audit">
          <legend class="field-label">Audit readers (optional)</legend>
          <div class="field-audit-header">
            <button
              v-if="auditMembers.length > 0"
              type="button"
              class="audit-select-all"
              @click="toggleSelectAllAuditMembers"
            >
              {{ allAuditMembersSelected ? 'Clear all' : 'Select all' }}
            </button>
          </div>
          <p class="field-help">
            Select the people in this establishment who must confirm they have read the document.
          </p>

          <p v-if="isLoadingAuditMembers" class="field-help">Loading available readers...</p>
          <p v-else-if="auditMembersErrorMessage" class="feedback-message feedback-message-error">
            {{ auditMembersErrorMessage }}
          </p>
          <p v-else-if="auditMembers.length === 0" class="field-help">
            No active establishment members are available for audit acknowledgement.
          </p>

          <div v-else class="audit-member-list">
            <label
              v-for="member in auditMembers"
              :key="member.id"
              class="audit-member-option"
            >
              <input
                v-model="selectedAuditUserIds"
                :value="member.userId"
                type="checkbox"
              />
              <span class="audit-member-copy">
                <span class="audit-member-name">{{ formatMemberName(member) }}</span>
                <span class="audit-member-email">{{ member.userEmail }}</span>
              </span>
            </label>
          </div>
        </fieldset>
      </div>
      <p v-if="errorMessage" class="feedback-message feedback-message-error">
        {{ errorMessage }}
      </p>

      <div class="form-actions">
        <RouterLink :to="{ name: backRouteName }" class="secondary-link">
          {{ backLinkLabel }}
        </RouterLink>

        <BaseButton class="submit-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Uploading...' : 'Upload document' }}
        </BaseButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page-header,
.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.page-header h1,
.page-subtitle,
.placeholder-panel h2,
.placeholder-panel p,
.feedback-message {
  margin: 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  max-width: 64ch;
}

.placeholder-panel h2 {
  font-size: var(--font-size-heading-md);
  line-height: var(--line-height-tight);
}

.placeholder-panel {
  padding: 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-file {
  grid-column: 1 / -1;
}

.field-audit {
  grid-column: 1 / -1;
  padding: 0;
  border: none;
  margin: 0;
}

.field-audit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.field-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-label);
  font-weight: 600;
  letter-spacing: var(--field-label-letter-spacing);
  text-transform: uppercase;
}

.audit-select-all {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-primary);
  cursor: pointer;
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
}

.audit-select-all:hover {
  text-decoration: underline;
}

.field-input {
  width: 100%;
  min-height: var(--field-min-height);
  padding: var(--field-padding-y) var(--field-padding-x);
  border: 1px solid var(--field-border-color);
  border-radius: var(--field-radius);
  background-color: var(--field-background);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.field-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--field-focus-ring);
}

.field-input-error {
  border-color: var(--color-critical);
}

.field-input-error:focus {
  border-color: var(--color-critical);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-critical) 18%, transparent);
}

.field-input-file {
  padding: 0.75rem var(--field-padding-x);
}

.field-help {
  color: var(--color-text-secondary);
  font-size: var(--font-size-body-sm);
}

.field-error {
  color: var(--color-critical);
  font-size: var(--font-size-body-sm);
}

.audit-member-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.audit-member-option {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 0.875rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
}

.audit-member-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.audit-member-name {
  font-weight: 600;
}

.audit-member-email {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.feedback-message-error {
  color: var(--color-critical);
  font-size: var(--font-size-body-sm);
  line-height: var(--line-height-body);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.back-link {
  align-self: flex-start;
  margin-top: 8px;
  text-decoration: none;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-xs);
  background-color: var(--color-primary);
  color: var(--color-white);
  font-size: 0.875rem;
  font-weight: 600;
}

.back-link:hover {
  background-color: color-mix(in srgb, var(--color-primary) 88%, black);
}

.secondary-link {
  text-decoration: none;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.secondary-link:hover {
  color: var(--color-text-primary);
}

.submit-button {
  width: auto;
  min-width: 0;
}

.submit-button :deep(.button) {
  width: auto;
  justify-content: center;
  padding: 0.875rem 1rem;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .audit-member-list {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .submit-button {
    width: 100%;
  }

  .submit-button :deep(.button) {
    width: 100%;
  }
}
</style>
