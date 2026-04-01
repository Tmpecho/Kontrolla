<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'

defineOptions({
  name: 'AppSidebar',
})

type NavigationItem = {
  label: string
  routeName?: 'workspace-home' | 'ik-mat-dashboard' | 'ik-mat-documents' | 'ik-mat-deviation' | 'ik-alkohol-dashboard' | 'ik-alkohol-documents' | 'ik-alkohol-deviation'
}

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeRouteNamesByNavigationRoute = {
  'workspace-home': ['workspace-home'],
  'ik-mat-dashboard': ['ik-mat-dashboard'],
  'ik-mat-documents': ['ik-mat-documents'],
  'ik-mat-deviation': ['ik-mat-deviation', 'ik-mat-deviation-form'],
  'ik-alkohol-dashboard': ['ik-alkohol-dashboard'],
  'ik-alkohol-documents': ['ik-alkohol-documents', 'ik-alkohol-documents-upload'],
  'ik-alkohol-deviation': ['ik-alkohol-deviation', 'ik-alkohol-deviation-form'],
} as const

function isNavigationItemActive(routeName?: NavigationItem['routeName']) {
  if (!routeName) {
    return false
  }

  const currentRouteName = typeof route.name === 'string' ? route.name : ''
  return activeRouteNamesByNavigationRoute[routeName].some(
    (candidateRouteName) => candidateRouteName === currentRouteName,
  )
}

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
          label: 'Important Documents',
          routeName: 'ik-mat-documents'
        },
        {
          label: 'Deviations',
          routeName: 'ik-mat-deviation'
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
          routeName: 'ik-alkohol-documents'
        },
        {
          label: 'Deviations',
          routeName: 'ik-alkohol-deviation'
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
              :data-active="isNavigationItemActive(navigationItem.routeName)"
            >
              {{ navigationItem.label }}
            </RouterLink>
            <span v-else class="nav-link nav-link-placeholder">{{ navigationItem.label }}</span>
          </li>
        </ul>
      </nav>
    </div>

    <div class="info-container">
      <!-- Maybe change this to normal svg icons -->
      <button type="button" class="sidebar-action sidebar-action-support">
        <span class="sidebar-action-icon" aria-hidden="true">
          <svg viewBox="0 0 20 20">
            <path
              d="M10 17a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="M7.9 7.5a2.35 2.35 0 1 1 4.2 1.45c-.45.56-1.1.94-1.6 1.42-.33.3-.5.63-.5 1.13"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="M10 14h.01"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </svg>
        </span>
        <span>Support</span>
      </button>

      <button type="button" class="sidebar-action sidebar-action-signout" @click="onLogout">
        <span class="sidebar-action-icon" aria-hidden="true">
          <svg viewBox="0 0 20 20">
            <path
              d="M8 4H5.75A1.75 1.75 0 0 0 4 5.75v8.5A1.75 1.75 0 0 0 5.75 16H8"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="M11.5 6.5 15 10l-3.5 3.5"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="M8 10h7"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
          </svg>
        </span>
        <span>Sign out</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.sidebar-container {
  display: flex;
  width: 200px;
  height: 100%;
  flex-shrink: 0;
  flex-direction: column;
  padding: 24px 20px;
  background-color: var(--color-white);
  overflow: hidden;
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

.nav-link[data-active='true'] {
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
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-muted);
}

.sidebar-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
}

.sidebar-action-icon {
  display: inline-flex;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.sidebar-action-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
}

.sidebar-action-support {
  color: var(--color-text-secondary);
}

.sidebar-action-support:hover {
  color: var(--color-text-primary);
}

.sidebar-action-signout {
  color: var(--color-critical);
}
</style>
