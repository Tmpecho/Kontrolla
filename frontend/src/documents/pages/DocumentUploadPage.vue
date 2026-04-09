<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { createDocument } from '@/documents/api/documents.api'
import type { DocumentServiceArea } from '@/documents/model/document.types'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'
import { appEnv } from '@/shared/config/env'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  holderName: '',
  issueDate: '',
  renewalDate: '',
})

const selectedFile = ref<File | null>(null)
const validationMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)
const isSubmitting = ref(false)

const isAlcoholPage = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return routeName.startsWith('ik-alkohol-')
})

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(
  () => authStore.appContext?.establishmentId ?? appEnv.defaultEstablishmentId ?? null,
)

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
  if (organizationId.value && establishmentId.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Documents cannot be uploaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID or sign in with an organization context to upload documents.'
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

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  validationMessage.value = null
}

function validateForm() {
  if (
    !form.title.trim() ||
    !form.holderName.trim() ||
    !form.issueDate ||
    !form.renewalDate
  ) {
    validationMessage.value = 'Complete all fields before uploading.'
    return false
  }

  if (!selectedFile.value) {
    validationMessage.value = 'Choose a PDF file to upload.'
    return false
  }

  const fileName = selectedFile.value.name.toLowerCase()
  const contentType = selectedFile.value.type.toLowerCase()

  if (contentType !== 'application/pdf' && !fileName.endsWith('.pdf')) {
    validationMessage.value = 'Only PDF files are supported.'
    return false
  }

  validationMessage.value = null
  return true
}

async function submitForm() {
  if (isSubmitting.value) {
    return
  }

  if (!validateForm()) {
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
            type="text"
            maxlength="255"
          />
        </label>

        <label class="field">
          <span class="field-label">Holder name</span>
          <input
            v-model="form.holderName"
            class="field-input"
            type="text"
            maxlength="255"
          />
        </label>

        <label class="field">
          <span class="field-label">Issue date</span>
          <input v-model="form.issueDate" class="field-input" type="date" />
        </label>

        <label class="field">
          <span class="field-label">Renewal date</span>
          <input v-model="form.renewalDate" class="field-input" type="date" />
        </label>

        <label class="field field-file">
          <span class="field-label">PDF file</span>
          <input
            class="field-input field-input-file"
            type="file"
            accept="application/pdf,.pdf"
            @change="onFileChange"
          />
          <span class="field-help">
            {{ selectedFile ? selectedFile.name : 'Select a PDF file to upload.' }}
          </span>
        </label>
      </div>

      <p v-if="validationMessage" class="feedback-message feedback-message-error">
        {{ validationMessage }}
      </p>
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

.field-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-label);
  font-weight: 600;
  letter-spacing: var(--field-label-letter-spacing);
  text-transform: uppercase;
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

.field-input-file {
  padding: 0.75rem var(--field-padding-x);
}

.field-help {
  color: var(--color-text-secondary);
  font-size: var(--font-size-body-sm);
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
