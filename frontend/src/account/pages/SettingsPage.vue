<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { changeMyPassword } from '@/account/api/account.api'
import { useAuthStore } from '@/auth/model/auth.store'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

const authStore = useAuthStore()
const router = useRouter()
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)
const attemptedSubmit = ref(false)

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const validationMessage = computed(() => {
  if (!form.currentPassword.trim()) {
    return 'Enter your current password.'
  }

  if (!form.newPassword.trim()) {
    return 'Enter a new password.'
  }

  if (form.newPassword.length < 8) {
    return 'New password must be at least 8 characters long.'
  }

  if (form.newPassword === form.currentPassword) {
    return 'New password must be different from the current password.'
  }

  if (!form.confirmNewPassword.trim()) {
    return 'Confirm your new password.'
  }

  if (form.confirmNewPassword !== form.newPassword) {
    return 'Password confirmation does not match.'
  }

  return null
})

const currentPasswordError = computed(() =>
  attemptedSubmit.value && !form.currentPassword.trim() ? 'Enter your current password.' : null,
)
const newPasswordError = computed(() => {
  if (!attemptedSubmit.value) {
    return null
  }

  if (!form.newPassword.trim()) {
    return 'Enter a new password.'
  }

  if (form.newPassword.length < 8) {
    return 'New password must be at least 8 characters long.'
  }

  if (form.newPassword === form.currentPassword) {
    return 'New password must be different from the current password.'
  }

  return null
})
const confirmPasswordError = computed(() => {
  if (!attemptedSubmit.value) {
    return null
  }

  if (!form.confirmNewPassword.trim()) {
    return 'Confirm your new password.'
  }

  if (form.confirmNewPassword !== form.newPassword) {
    return 'Password confirmation does not match.'
  }

  return null
})

const isSubmitDisabled = computed(() => isSubmitting.value)

function clearError() {
  errorMessage.value = null
}

async function onSubmit() {
  if (validationMessage.value) {
    attemptedSubmit.value = true
    errorMessage.value = null
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    await changeMyPassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    await authStore.logout()
    await router.push({
      name: 'login',
      query: {
        passwordChanged: '1',
      },
    })
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to update password.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="settings-page app-page">
    <header class="page-header app-page-header">
      <div class="app-page-header-copy">
        <h1 class="app-page-title">Settings</h1>
        <p class="app-page-subtitle">Account security settings for your signed-in profile.</p>
      </div>
    </header>

    <section class="settings-panel app-panel">
      <div class="panel-header">
        <h2>Password</h2>
        <p>
          Change your password and sign in again with the updated credentials across the app.
        </p>
      </div>

      <form class="settings-form" @submit.prevent="onSubmit">
        <p v-if="errorMessage" class="feedback-message">{{ errorMessage }}</p>

        <BaseInput
          id="current-password"
          v-model="form.currentPassword"
          label="Current password"
          type="password"
          :error="currentPasswordError"
          autocomplete="current-password"
          placeholder="Current password"
          @update:model-value="clearError"
        />

        <BaseInput
          id="new-password"
          v-model="form.newPassword"
          label="New password"
          type="password"
          :error="newPasswordError"
          autocomplete="new-password"
          placeholder="New password"
          hint="Use at least 8 characters."
          @update:model-value="clearError"
        />

        <BaseInput
          id="confirm-new-password"
          v-model="form.confirmNewPassword"
          label="Confirm new password"
          type="password"
          :error="confirmPasswordError"
          autocomplete="new-password"
          placeholder="Confirm new password"
          @update:model-value="clearError"
        />

        <div class="actions-row">
          <BaseButton type="submit" :disabled="isSubmitDisabled">
            {{ isSubmitting ? 'Updating password...' : 'Update password' }}
          </BaseButton>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.page-header,
.settings-panel,
.settings-form,
.panel-header {
  display: flex;
  flex-direction: column;
}

.page-header,
.panel-header {
  gap: 8px;
}

.page-header h1,
.page-header p,
.panel-header h2,
.panel-header p,
.feedback-message {
  margin: 0;
}

.settings-panel {
  gap: 20px;
  max-width: 720px;
  padding: 24px;
}

.panel-header p {
  color: var(--color-text-secondary);
}

.settings-form {
  gap: 16px;
}

.feedback-message {
  padding: 12px 14px;
  border: 1px solid var(--color-critical);
  border-radius: var(--radius-xs);
  color: var(--color-critical);
  font-size: 0.9375rem;
}

.actions-row {
  max-width: 260px;
}

@media (max-width: 720px) {
  .actions-row {
    max-width: none;
  }
}
</style>
