<script setup lang="ts">
import LoginForm from '@/auth/components/LoginForm.vue'
import { appEnv } from '@/shared/config/env'
</script>

<template>
  <header>
    <h1>Log in</h1>
    <p>Sign in to Kontrolla.</p>
  </header>

  <p v-if="appEnv.showDevLoginHint">
    Development login: <code>demo@example.com</code> / <code>password123</code>
  </p>

  <LoginForm />
</template>
