<script setup lang="ts">
import LoginForm from '@/auth/components/LoginForm.vue'
import { appEnv } from '@/shared/config/env'
</script>
<template>
  <div class="page-layout">
    <div class="main-content">
      <header class="brand-header">
        <div class="brand-icon-wrapper">
          <svg class="brand-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
            />
          </svg>
        </div>
        <h1 class="brand-title">Kontrolla</h1>
        <p class="brand-subtitle">PORTAL LOGIN</p>
      </header>
      <div class="form-wrapper">
        <div v-if="appEnv.showDevLoginHint" class="dev-hint">
          <span>Dev login:</span>
          <code>demo@example.com</code>
          <code>password123</code>
        </div>
        <div class="form-box">
          <LoginForm />
        </div>
      </div>
    </div>
  </div>
</template>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.page-layout {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-surface);
  padding: 2rem 2rem 10vh;
  box-sizing: border-box;
}

.main-content {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.brand-header {
  text-align: center;
}

.brand-icon-wrapper {
  display: inline-flex;
  padding: 12px;
  background-color: var(--color-container);
  border-radius: 3px;
  margin-bottom: 1rem;
}

.brand-icon {
  width: 28px;
  height: 28px;
  color: var(--color-primary);
}

.brand-title {
  font-size: 1.75rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.brand-subtitle {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-top: 0.25rem;
}

.form-wrapper {
  position: relative;
  width: 100%;
}

.form-box {
  background-color: var(--color-container);
  border-radius: 5px;
  padding: 2.5rem;
  width: 100%;
  box-sizing: border-box;
}

.dev-hint {
  position: absolute;
  top: 0;
  left: calc(-210px - 1.5rem);
  width: 210px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  background-color: var(--color-container);
  border-left: 4px solid var(--color-success);
  border-radius: 4px;
  padding: 1rem;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  box-sizing: border-box;
  z-index: 10;
}
</style>
