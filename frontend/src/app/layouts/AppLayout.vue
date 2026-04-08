<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import EstablishmentSwitcher from '@/app/components/EstablishmentSwitcher.vue'
import TopBar from '@/app/components/TopBar.vue'
import Sidebar from '@/app/components/Sidebar.vue'
import { useAuthStore } from '@/auth/model/auth.store'

const route = useRoute()
const authStore = useAuthStore()
const topBar = ref<InstanceType<typeof TopBar> | null>(null)
const mobileNavigationDrawer = ref<HTMLElement | null>(null)
const isMobileNavigationOpen = ref(false)
const previousBodyOverflow = ref<string | null>(null)

function handleMobileNavigationToggle() {
  if (isMobileNavigationOpen.value) {
    closeMobileNavigation()
    return
  }

  isMobileNavigationOpen.value = true
}

function closeMobileNavigation(returnFocus = true) {
  if (!isMobileNavigationOpen.value) {
    return
  }

  isMobileNavigationOpen.value = false

  if (returnFocus) {
    void nextTick(() => {
      topBar.value?.focusMobileNavTrigger()
    })
  }
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && isMobileNavigationOpen.value) {
    closeMobileNavigation()
  }
}

function getFocusableDrawerElements() {
  return mobileNavigationDrawer.value?.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )
}

function handleMobileNavigationKeydown(event: KeyboardEvent) {
  if (!isMobileNavigationOpen.value || event.key !== 'Tab') {
    return
  }

  const focusableElements = getFocusableDrawerElements()

  if (!focusableElements || focusableElements.length === 0) {
    event.preventDefault()
    mobileNavigationDrawer.value?.focus()
    return
  }

  const firstFocusableElement = focusableElements.item(0)
  const lastFocusableElement = focusableElements.item(focusableElements.length - 1)
  const activeElement = document.activeElement

  if (!firstFocusableElement || !lastFocusableElement) {
    event.preventDefault()
    mobileNavigationDrawer.value?.focus()
    return
  }

  if (event.shiftKey && activeElement === mobileNavigationDrawer.value) {
    event.preventDefault()
    lastFocusableElement.focus()
    return
  }

  if (event.shiftKey && activeElement === firstFocusableElement) {
    event.preventDefault()
    lastFocusableElement.focus()
    return
  }

  if (!event.shiftKey && activeElement === lastFocusableElement) {
    event.preventDefault()
    firstFocusableElement.focus()
  }
}

function handleResize() {
  if (window.innerWidth > 960 && isMobileNavigationOpen.value) {
    closeMobileNavigation(false)
  }
}

watch(
  () => route.fullPath,
  () => {
    closeMobileNavigation(false)
  },
)

watch(isMobileNavigationOpen, async (isOpen) => {
  if (isOpen) {
    previousBodyOverflow.value = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else if (previousBodyOverflow.value !== null) {
    document.body.style.overflow = previousBodyOverflow.value
    previousBodyOverflow.value = null
  }

  if (isOpen) {
    await nextTick()
    mobileNavigationDrawer.value?.focus()
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleEscape)
  window.removeEventListener('resize', handleResize)

  if (previousBodyOverflow.value !== null) {
    document.body.style.overflow = previousBodyOverflow.value
    previousBodyOverflow.value = null
  }
})
</script>

<template>
  <div class="app-shell">
    <TopBar
      ref="topBar"
      :mobile-nav-open="isMobileNavigationOpen"
      @toggle-mobile-nav="handleMobileNavigationToggle"
    />

    <div class="app-body">
      <Sidebar class="desktop-sidebar" />

      <div
        v-if="isMobileNavigationOpen"
        class="mobile-navigation-layer"
      >
        <div class="mobile-navigation-backdrop" @click="closeMobileNavigation()" />
        <aside
          id="mobile-navigation"
          ref="mobileNavigationDrawer"
          class="mobile-navigation-drawer"
          role="dialog"
          aria-modal="true"
          aria-label="App navigation"
          tabindex="-1"
          @keydown="handleMobileNavigationKeydown"
        >
          <Sidebar variant="mobile" @navigate="closeMobileNavigation(false)" />
        </aside>
      </div>

      <main class="app-content" :class="{ 'app-content--nav-open': isMobileNavigationOpen }">
        <section
          v-if="authStore.requiresEstablishmentSelection"
          class="establishment-selection-banner"
        >
          <div>
            <h2>Select establishment</h2>
            <p>Choose which establishment you are currently working in before continuing.</p>
          </div>
          <EstablishmentSwitcher variant="panel" />
        </section>
        <RouterView />
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

.mobile-navigation-layer {
  display: none;
}

.app-content {
  flex: 1;
  padding: 24px;
  min-width: 0;
  overflow-y: auto;
}

.establishment-selection-banner {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.establishment-selection-banner h2,
.establishment-selection-banner p {
  margin: 0;
}

.establishment-selection-banner p {
  color: var(--color-text-secondary);
}

@media (max-width: 960px) {
  .desktop-sidebar {
    display: none;
  }

  .mobile-navigation-layer {
    position: absolute;
    inset: 0;
    z-index: 20;
    display: block;
  }

  .mobile-navigation-backdrop {
    position: absolute;
    inset: 0;
    background-color: rgba(15, 23, 42, 0.32);
  }

  .mobile-navigation-drawer {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: min(86vw, 320px);
    background-color: var(--color-white);
    box-shadow: var(--shadow-elevated);
    outline: none;
  }

  .app-content {
    padding: 20px 16px;
  }

  .app-content--nav-open {
    overflow: hidden;
  }
}
</style>
