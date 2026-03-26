<script setup lang="ts">
// Your existing script block with all the auth logic
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/auth/model/auth.store'

import Input from '@/shared/components/Input.vue'
import Button from '@/shared/components/Button.vue'

const authStore = useAuthStore()
const router = useRouter()
const form = reactive({ email: '', password: '' })
const errorMessage = ref('')
const isSubmitting = ref(false)

async function onSubmit() {
  errorMessage.value = ''
  isSubmitting.value = true
  try {
    await authStore.login({ email: form.email, password: form.password })
    await router.push({ name: 'workspace-home' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to log in'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="onSubmit" class="form-wrapper">
    <div v-if="errorMessage" class="error-notification">{{ errorMessage }}</div>

    <div class="input-group">
      <Input
        id="email"
        label="Email or Username"
        type="email"
        autocomplete="email"
        v-model="form.email"
      />
    </div>

    <div class="input-group">
      <Input
        id="password"
        label="Password"
        type="password"
        autocomplete="current-password"
        v-model="form.password"
      >
        <template #aside>
          <a href="#" class="input-aside-link">Forgot Password?</a>
        </template>
      </Input>
    </div>

    <div class="actions">
      <Button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? 'Signing In...' : 'Sign In' }} <span v-if="!isSubmitting">&rarr;</span>
      </Button>
    </div>

    <footer class="form-footer">
      Having trouble signing in? <a href="#" class="footer-link">Contact Support &nearr;</a>
    </footer>
  </form>
</template>

<style scoped>
.form-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-aside-link {
  font-size: 0.875rem;
  text-decoration: none;
  color: #3763f4;
  font-weight: 500;
}

.input-aside-link:hover {
  text-decoration: underline;
}

.actions {
  margin-top: 0.5rem;
}

.form-footer {
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 1rem;
}

.footer-link {
  color: #3763f4;
  font-weight: 500;
  text-decoration: none;
}
.footer-link:hover {
  text-decoration: underline;
}

.error-notification {
  background-color: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  text-align: center;
}
</style>
