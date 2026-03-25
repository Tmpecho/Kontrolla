<script setup lang="ts">
import LoginForm from '@/auth/components/LoginForm.vue'
import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()
</script>

<template>
  <main>
    <section>
      <header>
        <h1>Log in</h1>
        <p>Sign in to Kontrolla.</p>
      </header>

      <LoginForm />

      <p v-if="authStore.isAuthenticated && authStore.user">
        Logged in as {{ authStore.user.email }}.
      </p>
    </section>
  </main>
</template>
