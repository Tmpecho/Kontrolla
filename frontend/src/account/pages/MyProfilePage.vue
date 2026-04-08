<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()
const router = useRouter()
const canManageMembers = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN'
  )
})

const fullName = computed(() => {
  if (!authStore.user) {
    return 'Not signed in'
  }

  return `${authStore.user.firstName} ${authStore.user.lastName}`
})

function goToOrganizationMembers() {
  void router.push({ name: 'organization-members' })
}
</script>

<template>
  <div class="account-page">
    <header class="page-header">
      <h1>My profile</h1>
      <p>Your personal account information.</p>
    </header>

    <section class="details-panel">
      <div v-if="canManageMembers" class="profile-actions">
        <button type="button" class="manage-members-button" @click="goToOrganizationMembers">
          Manage organization members
        </button>
      </div>

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

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-muted);
}

.manage-members-button {
  width: fit-content;
  min-height: 42px;
  padding: 0.8rem 1rem;
  border: 0;
  border-radius: 4px;
  background-color: #1557b0;
  color: #fff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.profile-hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
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
