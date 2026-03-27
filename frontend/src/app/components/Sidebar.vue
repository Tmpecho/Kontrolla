<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { getEstablishment } from '@/establishments/api/establishments.api'
import { getOrganization } from '@/organizations/api/organizations.api'
import { appEnv } from '@/shared/config/env'

defineOptions({
  name: 'AppSidebar',
})

type NavigationItem = {
  label: string
  routeName: 'workspace-home' | 'ik-mat-dashboard' | 'ik-alkohol-dashboard'
}

const authStore = useAuthStore()
const router = useRouter()
const organizationName = ref<string | null>(null)
const establishmentName = ref<string | null>(null)
const isContextLoading = ref(false)
const contextErrorMessage = ref<string | null>(null)

const navigationItems: NavigationItem[] = []

const resolvedOrganizationName = computed(() => organizationName.value ?? 'Organization')
const resolvedEstablishmentName = computed(() => establishmentName.value ?? 'Establishment')

async function loadContext() {
  const organizationId = appEnv.defaultOrganizationId
  const establishmentId = appEnv.defaultEstablishmentId

  if (!organizationId || !establishmentId) {
    contextErrorMessage.value = appEnv.isDevelopment
      ? 'Set default organization and establishment IDs to load app context.'
      : 'Organization context is unavailable.'
    return
  }

  isContextLoading.value = true
  contextErrorMessage.value = null

  try {
    const [organization, establishment] = await Promise.all([
      getOrganization(organizationId),
      getEstablishment(organizationId, establishmentId),
    ])

    organizationName.value = organization.name
    establishmentName.value = establishment.name
  } catch (error) {
    contextErrorMessage.value =
      error instanceof Error ? error.message : 'Unable to load app context.'
  } finally {
    isContextLoading.value = false
  }
}

async function onLogout() {
  await authStore.logout()
  await router.push({ name: 'login' })
}

onMounted(() => {
  void loadContext()
})
</script>

<template>
  <div class="sidebar-container">
    <div class="establishment-info">
      <h2>{{ resolvedOrganizationName }}</h2>
      <p>{{ resolvedEstablishmentName }}</p>
      <p v-if="isContextLoading" class="sidebar-meta">Loading context...</p>
      <p v-else-if="contextErrorMessage" class="sidebar-meta">{{ contextErrorMessage }}</p>
    </div>

    <nav aria-label="App navigation" class="subservices">
      <ul>
        <li v-for="navigationItem in navigationItems" :key="navigationItem.routeName">
          <RouterLink :to="{ name: navigationItem.routeName }" class="nav-link">
            {{ navigationItem.label }}
          </RouterLink>
        </li>
      </ul>
    </nav>

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

.establishment-info h2 {
  color: var(--color-primary);
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
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.nav-link {
  display: block;
  padding: 10px 12px;
  border-radius: 6px;
  color: var(--color-text-primary);
  text-decoration: none;
}

.nav-link.router-link-active {
  background-color: var(--color-surface);
  color: var(--color-primary);
}

.info-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-container button {
  width: 100%;
}
</style>
