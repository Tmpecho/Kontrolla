<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/auth/model/auth.store'

import Input from '@/shared/components/Input.vue'
import Button from '@/shared/components/Button.vue'

const authStore = useAuthStore()
const router = useRouter()

const form = reactive({
  email: '',
  password: '',
})

const errorMessage = ref('')
const isSubmitting = ref(false)

async function onSubmit() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await authStore.login({
      email: form.email,
      password: form.password,
    })
    await router.push({ name: 'workspace-home' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to log in'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="onSubmit" class="inner-form">
    <div class="form-content">
      <div v-if="errorMessage" class="error-notification" role="alert">
        <span class="error-icon" aria-hidden="true">!</span>
        <div class="error-text">{{ errorMessage }}</div>
      </div>

      <div class="input-wrapper">
        <Input id="email" label="Email address" type="email" v-model="form.email" />
      </div>

      <div class="input-wrapper">
        <Input id="password" label="Password" type="password" v-model="form.password" />
      </div>
    </div>

    <div class="form-actions">
      <div class="empty-action"></div>

      <div class="primary-action">
        <Button type="submit" class="continue-btn" :disabled="isSubmitting">
          {{ isSubmitting ? 'Logging in...' : 'Continue' }}
          <span v-if="!isSubmitting" class="arrow">&rarr;</span>
        </Button>
      </div>
    </div>
  </form>
</template>

<style scoped>
.inner-form {
  display: flex;
  flex-direction: column;
}

.form-content {
  padding: 0 3rem 1.5rem 3rem;
}

.error-notification {
  background-color: #fff1f1;
  border-left: 4px solid #da1e28;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.error-icon {
  color: #da1e28;
  font-weight: 700;
  font-size: 1rem;
  line-height: 1.2;
}

.error-text {
  font-size: 0.875rem;
  color: #161616;
  line-height: 1.4;
}

.input-wrapper {
  position: relative;
  margin-bottom: 1.5rem;
}

.forgot-link {
  position: absolute;
  top: 0;
  right: 0;
  color: #0f62fe;
  text-decoration: none;
  font-size: 0.875rem;
}

.forgot-link:hover {
  text-decoration: underline;
}

.form-actions {
  display: flex;
  width: 100%;
  height: 4rem;
}

.empty-action {
  flex: 1;
  border-top: 1px solid #e0e0e0;
}

.primary-action {
  flex: 1;
}

.primary-action :deep(button),
.continue-btn {
  width: 100%;
  height: 100%;
  border-radius: 0;
  background-color: #0f62fe;
  color: white;
  border: none;
  font-size: 0.875rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-action :deep(button):hover,
.continue-btn:hover {
  background-color: #0353e9;
}

.primary-action :deep(button:disabled),
.continue-btn:disabled {
  background-color: #8d8d8d;
  cursor: not-allowed;
}

.arrow {
  font-size: 1.125rem;
  line-height: 1;
}
</style>
