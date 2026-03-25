<script setup lang="ts">
import { reactive, ref } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()

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
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to log in'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="onSubmit">
    <div>
      <label for="email">Email</label>
      <input id="email" v-model="form.email" type="email" autocomplete="email" />
    </div>

    <div>
      <label for="password">Password</label>
      <input
        id="password"
        v-model="form.password"
        type="password"
        autocomplete="current-password"
      />
    </div>

    <p v-if="errorMessage">{{ errorMessage }}</p>

    <div>
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? 'Logging in...' : 'Log in' }}
      </button>
    </div>
  </form>
</template>
