<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()

const fullName = computed(() => {
  if (!authStore.user) {
    return 'Not signed in'
  }

  return `${authStore.user.firstName} ${authStore.user.lastName}`
})
</script>

<template>
  <div class="account-page">
    <header class="page-header">
      <h1>My profile</h1>
      <p>Your personal account information.</p>
    </header>

    <section class="details-panel">
      <div class="detail-row">
        <span class="detail-label">Name</span>
        <span class="detail-value">{{ fullName }}</span>
      </div>

      <div class="detail-row">
        <span class="detail-label">Email</span>
        <span class="detail-value">{{ authStore.user?.email ?? 'Unavailable' }}</span>
      </div>

      <div class="detail-row">
        <span class="detail-label">Organization</span>
        <span class="detail-value">
          {{ authStore.appContext?.organizationName ?? 'Unavailable' }}
        </span>
      </div>

      <div class="detail-row">
        <span class="detail-label">Establishment</span>
        <span class="detail-value">
          {{ authStore.appContext?.establishmentName ?? 'Unavailable' }}
        </span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.details-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-header h1,
.page-header p {
  margin: 0;
}

.details-panel {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.detail-value {
  color: var(--color-text-primary);
}
</style>
