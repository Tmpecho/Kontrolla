<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()
const route = useRoute()

const primaryDestination = computed(() => {
  if (authStore.isAuthenticated) {
    return {
      label: 'Go to workspace',
      routeName: 'workspace-home',
    } as const
  }

  return {
    label: 'Go to homepage',
    routeName: 'landing',
  } as const
})
</script>

<template>
  <div class="not-found-page">
    <div class="not-found-panel">
      <p class="status-code">404</p>
      <h1>Page not found</h1>
      <p class="description">
        The page you requested does not exist or may have been moved.
      </p>
      <p class="path-preview">{{ route.fullPath }}</p>

      <div class="action-row">
        <RouterLink :to="{ name: primaryDestination.routeName }" class="primary-action">
          {{ primaryDestination.label }}
        </RouterLink>

        <RouterLink
          v-if="!authStore.isAuthenticated"
          :to="{ name: 'login' }"
          class="secondary-action"
        >
          Log in
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  display: flex;
  min-height: calc(100vh - 120px);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px;
  box-sizing: border-box;
  text-align: center;
}

.not-found-panel {
  display: flex;
  width: min(100%, 640px);
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: #f1f5f9;
  box-sizing: border-box;
}

.status-code,
.description,
.path-preview {
  margin: 0;
}

.status-code {
  color: var(--color-primary);
  font-size: clamp(5rem, 14vw, 8rem);
  font-weight: 600;
  letter-spacing: 0.08em;
  line-height: 0.9;
}

h1 {
  margin: 0;
  color: var(--color-text-primary);
}

.description {
  color: var(--color-text-secondary);
  max-width: 48ch;
}

.path-preview {
  color: var(--color-text-secondary);
  font-family: monospace;
  font-size: 0.875rem;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.875rem 1rem;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.875rem;
}

.primary-action {
  background-color: var(--color-primary);
  color: var(--color-white);
}

.secondary-action {
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-muted);
  background-color: var(--color-container);
}
</style>
