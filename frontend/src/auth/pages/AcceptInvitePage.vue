<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { acceptInvite, AuthApiError, getInviteDetails } from '@/auth/api/auth.api'
import type { InviteDetails } from '@/auth/model/auth.types'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

const route = useRoute()
const router = useRouter()

const token = computed(() => String(route.params.token ?? ''))
const invite = ref<InviteDetails | null>(null)
const isLoading = ref(true)
const isSubmitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const form = reactive({
  password: '',
  confirmPassword: '',
})

const passwordMismatch = computed(() => {
  return form.confirmPassword.length > 0 && form.password !== form.confirmPassword
})

async function loadInvite(): Promise<void> {
  isLoading.value = true
  errorMessage.value = ''

  try {
    invite.value = await getInviteDetails(token.value)
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Unable to load invitation details.'
  } finally {
    isLoading.value = false
  }
}

async function handleSubmit(): Promise<void> {
  if (passwordMismatch.value || form.password.length < 8) {
    errorMessage.value = 'Choose a password with at least 8 characters and confirm it correctly.'
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    await acceptInvite(token.value, form.password)
    successMessage.value = 'Your account is ready. You can now sign in with your email and password.'
    invite.value = null
  } catch (error) {
    errorMessage.value =
      error instanceof AuthApiError ? error.message : 'Unable to accept the invitation.'
  } finally {
    isSubmitting.value = false
  }
}

async function goToLogin(): Promise<void> {
  await router.push({ name: 'login' })
}

onMounted(() => {
  void loadInvite()
})
</script>

<template>
  <div class="invite-page">
    <section class="invite-panel">
      <header class="invite-header">
        <p class="invite-kicker">Organization Invitation</p>
        <h1>Set your password</h1>
      </header>

      <p v-if="isLoading" class="invite-message">Loading invitation...</p>

      <div v-else-if="successMessage" class="invite-success">
        <p>{{ successMessage }}</p>
        <button type="button" class="secondary-button" @click="goToLogin">Go to login</button>
      </div>

      <template v-else-if="invite">
        <div class="invite-summary">
          <div class="invite-row">
            <span class="invite-label">Organization</span>
            <span>{{ invite.organizationName }}</span>
          </div>
          <div class="invite-row">
            <span class="invite-label">Invited as</span>
            <span>{{ invite.firstName }} {{ invite.lastName }}</span>
          </div>
          <div class="invite-row">
            <span class="invite-label">Email</span>
            <span>{{ invite.email }}</span>
          </div>
        </div>

        <form class="invite-form" @submit.prevent="handleSubmit">
          <BaseInput
            id="invite-password"
            label="Password"
            type="password"
            autocomplete="new-password"
            :model-value="form.password"
            @update:model-value="form.password = $event"
            placeholder="Choose a password"
          />

          <BaseInput
            id="invite-password-confirm"
            label="Confirm password"
            type="password"
            autocomplete="new-password"
            :model-value="form.confirmPassword"
            @update:model-value="form.confirmPassword = $event"
            placeholder="Confirm password"
          />

          <p v-if="passwordMismatch" class="invite-error">Passwords do not match.</p>
          <p v-if="errorMessage" class="invite-error">{{ errorMessage }}</p>

          <div class="invite-actions">
            <BaseButton type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? 'Activating...' : 'Activate account' }}
            </BaseButton>
          </div>
        </form>
      </template>

      <p v-else class="invite-error">{{ errorMessage }}</p>
    </section>
  </div>
</template>

<style scoped>
.invite-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background-color: var(--color-surface);
  box-sizing: border-box;
}

.invite-panel {
  display: flex;
  width: 100%;
  max-width: 480px;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 6px;
  background-color: var(--color-container);
}

.invite-header,
.invite-form,
.invite-success {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.invite-kicker {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.invite-header h1,
.invite-message,
.invite-success p,
.invite-error {
  margin: 0;
}

.invite-summary {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
}

.invite-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.invite-label {
  color: var(--color-text-secondary);
}

.invite-error {
  color: var(--color-critical);
  font-size: 0.9rem;
}

.secondary-button {
  min-height: 42px;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  cursor: pointer;
}
</style>
