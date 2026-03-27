<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'

defineOptions({
  name: 'AppSidebar',
})

type NavigationItem = {
  label: string
  routeName?: 'workspace-home' | 'ik-mat-dashboard' | 'ik-alkohol-dashboard'
}

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const currentAppSection = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-mat-')) {
    return 'ik-mat'
  }

  if (routeName.startsWith('ik-alkohol-')) {
    return 'ik-alkohol'
  }

  return 'workspace'
})

const navigationItems = computed<NavigationItem[]>(() => {
  switch (currentAppSection.value) {
    case 'ik-mat':
      return [
        {
          label: 'Dashboard',
          routeName: 'ik-mat-dashboard',
        },
        {
          label: 'Checklists',
        },
        {
          label: 'Reports',
        },
      ]
    case 'ik-alkohol':
      return [
        {
          label: 'Dashboard',
          routeName: 'ik-alkohol-dashboard',
        },
        {
          label: 'Important Documents',
        },
      ]
    default:
      return [
        {
          label: 'Dashboard',
          routeName: 'workspace-home',
        },
      ]
  }
})

const displayOrganizationName = computed(() => {
  if (!authStore.isSessionReady) {
    return 'Loading organization...'
  }

  if (authStore.appContext?.organizationName) {
    return authStore.appContext.organizationName
  }

  return 'Organization unavailable'
})

const displayEstablishmentName = computed(() => {
  if (!authStore.isSessionReady) {
    return 'Loading establishment...'
  }

  if (authStore.appContext?.establishmentName) {
    return authStore.appContext.establishmentName
  }

  return 'Establishment unavailable'
})

async function onLogout() {
  await authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="sidebar-container">
    <div class="sidebar-content">
      <div class="establishment-info">
        <h2>{{ displayOrganizationName }}</h2>
        <p>{{ displayEstablishmentName }}</p>
      </div>

      <nav aria-label="App navigation" class="subservices">
        <ul>
          <li
            v-for="navigationItem in navigationItems"
            :key="navigationItem.routeName ?? navigationItem.label"
          >
            <RouterLink
              v-if="navigationItem.routeName"
              :to="{ name: navigationItem.routeName }"
              class="nav-link"
            >
              {{ navigationItem.label }}
            </RouterLink>
            <span v-else class="nav-link nav-link-placeholder">{{ navigationItem.label }}</span>
          </li>
        </ul>
      </nav>
    </div>

    <div class="info-container">
      <div>
        <button type="button">Support</button>
      </div>

      <div>
        <button type="button" @click="onLogout">Sign out</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar-container {
  display: flex;
  width: 200px;
  min-height: 100%;
  flex-direction: column;
  padding: 24px 20px;
  background-color: var(--color-white);
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.establishment-info h2 {
  color: var(--color-primary);
}

.establishment-info p {
  font-size: small;
}

.establishment-info h2,
.establishment-info p {
  margin: 0;
}

.establishment-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-eyebrow,
.sidebar-meta {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.subservices ul {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.nav-link {
  display: block;
  padding: 10px 12px;
  border-radius: 4px;
  color: var(--color-text-secondary);
  font-size: small;
  text-decoration: none;
}

.nav-link.router-link-active {
  background-color: var(--color-surface);
  color: var(--color-text-primary);
  font-weight: 500;
}

.nav-link-placeholder {
  color: var(--color-text-secondary);
}

.info-container {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-container button {
  width: 100%;
}
</style>
