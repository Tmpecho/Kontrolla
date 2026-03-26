<script setup lang="ts">
import LoginForm from '@/auth/components/LoginForm.vue'
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

      <div class="form-box">
        <LoginForm />
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

#app {
  height: 100%;
}
</style>

<style scoped>
.page-layout {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  padding: 2rem;

  padding-bottom: 10vh;
  box-sizing: border-box;

  font-family:
    system-ui,
    -apple-system,
    sans-serif;
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
  background-color: #eef2ff;
  border-radius: 3px;
  margin-bottom: 1rem;
}

.brand-icon {
  width: 28px;
  height: 28px;
  color: #3763f4;
}

.brand-title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.brand-subtitle {
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  margin-top: 0.25rem;
}

.form-box {
  background-color: #ffffff;
  border-radius: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  padding: 2.5rem;
}
</style>
