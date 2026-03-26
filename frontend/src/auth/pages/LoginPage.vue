<script setup lang="ts">
import LoginForm from '@/auth/components/LoginForm.vue'
import { appEnv } from '@/shared/config/env'
</script>

<template>
  <div class="page-layout">
    <div class="login-container">
      <header class="brand-header">
        <div class="brand-accent"></div>
        <span class="brand-name">Kontrolla</span>
      </header>

      <div class="form-box">
        <div class="form-header">
          <h1 class="title">Log in</h1>
          <div v-if="appEnv.showDevLoginHint" class="dev-hint">
            Dev login: <code>demo@example.com</code> / <code>password123</code>
          </div>
        </div>

        <LoginForm />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f4f4f4;
  padding: 2rem;
  font-family:
    'IBM Plex Sans',
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}

.login-container {
  width: 100%;
  max-width: 480px;
}

.form-box {
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.form-header {
  padding: 3rem 3rem 0 3rem;
}

.title {
  font-size: 2rem;
  font-weight: 300;
  color: #161616;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  font-size: 0.875rem;
  color: #525252;
  margin: 0 0 1.5rem 0;
}

.text-link {
  color: #0f62fe;
  text-decoration: none;
  font-size: 0.875rem;
}

.text-link:hover {
  text-decoration: underline;
}

.dev-hint {
  background-color: #defbe6;
  border-left: 4px solid #24a148;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  font-size: 0.875rem;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.brand-accent {
  width: 12px;
  height: 12px;
  background-color: #0f62fe;
}

.brand-name {
  font-size: 1.5rem;
  font-weight: 600;
  color: #161616;
  letter-spacing: 0.5px;
}
</style>
