<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppStartupState from '@/app/components/AppStartupState.vue'
import TopBar from '@/app/components/TopBar.vue'
import Sidebar from '@/app/components/Sidebar.vue'
import { useAuthStore } from '@/auth/model/auth.store'
import { useNotificationsStore } from '@/notifications/model/notifications.store'
import AppOverlay from '@/shared/components/overlay/AppOverlay.vue'

const route = useRoute()
const authStore = useAuthStore()
const isMobileNavigationOpen = ref(false)
const notificationsStore = useNotificationsStore()
const showStartupState = computed(() => {
  return authStore.isStartupPending || authStore.startupStatus === 'error'
})

function handleMobileNavigationToggle() {
  isMobileNavigationOpen.value = !isMobileNavigationOpen.value
}

function closeMobileNavigation() {
  if (!isMobileNavigationOpen.value) {
    return
  }

  isMobileNavigationOpen.value = false
}

function handleResize() {
  if (window.innerWidth > 960 && isMobileNavigationOpen.value) {
    closeMobileNavigation()
  }
}

watch(
  () => route.fullPath,
  () => {
    closeMobileNavigation()
  },
)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  notificationsStore.stopPolling()
})

watch(
  () => [authStore.isAuthenticated, authStore.startupStatus] as const,
  ([isAuthenticated, startupStatus]) => {
    if (isAuthenticated && startupStatus === 'ready') {
      notificationsStore.startPolling()
      return
    }

    notificationsStore.reset()
  },
  { immediate: true },
)
</script>

<template>
  <div class="app-shell">
    <TopBar :mobile-nav-open="isMobileNavigationOpen" @toggle-mobile-nav="handleMobileNavigationToggle" />

    <div class="app-body">
      <Sidebar class="desktop-sidebar" />

      <AppOverlay
        :open="isMobileNavigationOpen"
        aria-label="App navigation"
        panel-id="mobile-navigation"
        variant="drawer-left"
        @close="closeMobileNavigation"
      >
        <Sidebar variant="mobile" @navigate="closeMobileNavigation" />
      </AppOverlay>

      <main class="app-content" :class="{ 'app-content--nav-open': isMobileNavigationOpen }">
        <AppStartupState
          v-if="showStartupState"
          :status="authStore.startupStatus"
          :error-message="authStore.startupError"
          :started-at="authStore.startupStartedAt"
          @retry="authStore.retryWorkspaceStartup"
        />
        <RouterView v-else />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-surface);
  overflow: hidden;
}

.app-body {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.app-content {
  flex: 1;
  padding: 24px;
  min-width: 0;
  overflow-y: auto;
}

@media (max-width: 960px) {
  .desktop-sidebar {
    display: none;
  }

  .app-content {
    padding: 20px 16px;
  }

  .app-content--nav-open {
    overflow: hidden;
  }
}
</style>
