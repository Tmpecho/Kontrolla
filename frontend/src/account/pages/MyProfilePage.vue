<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { updateMyProfile } from '@/account/api/account.api'
import { useAuthStore } from '@/auth/model/auth.store'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

const authStore = useAuthStore()
const router = useRouter()
const isSaving = ref(false)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const attemptedSubmit = ref(false)

const form = reactive({
  firstName: '',
  lastName: '',
})

watch(
  () => authStore.user,
  (user) => {
    form.firstName = user?.firstName ?? ''
    form.lastName = user?.lastName ?? ''
  },
  { immediate: true },
)

const canManageMembers = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN'
  )
})

const fullName = computed(() => {
  const parts = [authStore.user?.firstName ?? '', authStore.user?.lastName ?? '']
    .map((value) => value.trim())
    .filter(Boolean)

  if (!parts.length) {
    return 'Not signed in'
  }

  return parts.join(' ')
})

const normalizedFirstName = computed(() => form.firstName.trim())
const normalizedLastName = computed(() => form.lastName.trim())

const validationMessage = computed(() => {
  if (!normalizedFirstName.value) {
    return 'Enter your first name.'
  }

  if (!normalizedLastName.value) {
    return 'Enter your last name.'
  }

  return null
})

const firstNameError = computed(() =>
  attemptedSubmit.value && !normalizedFirstName.value ? 'Enter your first name.' : null,
)
const lastNameError = computed(() =>
  attemptedSubmit.value && !normalizedLastName.value ? 'Enter your last name.' : null,
)

const hasProfileChanges = computed(() => {
  if (!authStore.user) {
    return false
  }

  return (
    normalizedFirstName.value !== authStore.user.firstName ||
    normalizedLastName.value !== authStore.user.lastName
  )
})

const isSubmitDisabled = computed(() => !authStore.user || isSaving.value)

const createdAtLabel = computed(() => formatDate(authStore.user?.createdAt))
const updatedAtLabel = computed(() => formatDate(authStore.user?.updatedAt))

function formatDate(value?: string | null): string {
  if (!value) {
    return 'Unavailable'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Unavailable'
  }

  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function clearFeedback() {
  errorMessage.value = null
  successMessage.value = null
}

function goToOrganizationMembers() {
  void router.push({ name: 'organization-members' })
}

async function onSubmit() {
  if (validationMessage.value) {
    attemptedSubmit.value = true
    errorMessage.value = null
    successMessage.value = null
    return
  }

  if (!hasProfileChanges.value) {
    successMessage.value = 'No changes to save.'
    errorMessage.value = null
    return
  }

  if (!authStore.user || isSaving.value) {
    return
  }

  isSaving.value = true
  errorMessage.value = null
  successMessage.value = null

  try {
    const updatedUser = await updateMyProfile({
      firstName: normalizedFirstName.value,
      lastName: normalizedLastName.value,
    })

    authStore.setCurrentUser(updatedUser)
    successMessage.value = 'Profile updated.'
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Failed to update profile.'
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="account-page">
    <header class="page-header">
      <h1>My profile</h1>
      <p>Your personal account information and the name shown across the workspace.</p>
    </header>

    <section class="details-panel">
      <div class="panel-header">
        <div class="panel-header-copy">
          <h2>Account summary</h2>
          <p>These details come from your authenticated account and current organization context.</p>
        </div>

        <div v-if="canManageMembers" class="profile-actions">
          <button type="button" class="manage-members-button" @click="goToOrganizationMembers">
            Manage organization members
          </button>
        </div>
      </div>

      <div class="details-grid">
        <div class="detail-row">
          <span class="detail-label">Name</span>
          <span class="detail-value">{{ fullName }}</span>
        </div>

        <div class="detail-row">
          <span class="detail-label">Email</span>
          <span class="detail-value">{{ authStore.user?.email ?? 'Unavailable' }}</span>
        </div>

        <div class="detail-row">
          <span class="detail-label">Organization</span>
          <span class="detail-value">
            {{ authStore.appContext?.organizationName ?? 'Unavailable' }}
          </span>
        </div>

        <div class="detail-row">
          <span class="detail-label">Establishment</span>
          <span class="detail-value">
            {{ authStore.appContext?.establishmentName ?? 'Unavailable' }}
          </span>
        </div>

        <div class="detail-row">
          <span class="detail-label">Account created</span>
          <span class="detail-value">{{ createdAtLabel }}</span>
        </div>

        <div class="detail-row">
          <span class="detail-label">Last updated</span>
          <span class="detail-value">{{ updatedAtLabel }}</span>
        </div>
      </div>
    </section>

    <section class="details-panel">
      <div class="panel-header">
        <div class="panel-header-copy">
          <h2>Personal details</h2>
          <p>Update the name that appears in the app for your account.</p>
        </div>
      </div>

      <form class="profile-form" @submit.prevent="onSubmit">
        <p v-if="errorMessage" class="feedback-message feedback-message-error">{{ errorMessage }}</p>
        <p v-else-if="successMessage" class="feedback-message feedback-message-success">
          {{ successMessage }}
        </p>

        <div class="form-grid">
          <BaseInput
            id="profile-first-name"
            v-model="form.firstName"
            label="First name"
            :error="firstNameError"
            autocomplete="given-name"
            placeholder="First name"
            @update:model-value="clearFeedback"
          />

          <BaseInput
            id="profile-last-name"
            v-model="form.lastName"
            label="Last name"
            :error="lastNameError"
            autocomplete="family-name"
            placeholder="Last name"
            @update:model-value="clearFeedback"
          />
        </div>

        <div class="actions-row">
          <BaseButton type="submit" :disabled="isSubmitDisabled">
            {{ isSaving ? 'Saving...' : 'Save changes' }}
          </BaseButton>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.details-panel,
.profile-form {
  display: flex;
  flex-direction: column;
}

.page-header {
  gap: 8px;
}

.page-header h1,
.page-header p,
.panel-header h2,
.panel-header p,
.feedback-message {
  margin: 0;
}

.details-panel {
  gap: 20px;
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.panel-header-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.panel-header h2 {
  font-size: var(--font-size-heading-md);
  line-height: var(--line-height-tight);
}

.panel-header p {
  color: var(--color-text-secondary);
}

.profile-actions {
  display: flex;
  align-items: flex-start;
}

.manage-members-button {
  min-height: 42px;
  padding: 0.8rem 1rem;
  border: 0;
  border-radius: 4px;
  background-color: #1557b0;
  color: #fff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.details-grid,
.form-grid {
  display: grid;
  gap: 16px;
}

.details-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.detail-value {
  color: var(--color-text-primary);
}

.profile-form {
  gap: 16px;
}

.feedback-message {
  padding: 12px 14px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  font-size: 0.9375rem;
}

.feedback-message-error {
  border-color: var(--color-critical);
  color: var(--color-critical);
}

.feedback-message-success {
  border-color: var(--color-success);
  color: var(--color-success);
}

.actions-row {
  max-width: 240px;
}

@media (max-width: 720px) {
  .details-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .actions-row {
    max-width: none;
  }
}
</style>
