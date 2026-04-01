<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppPopupShell from '@/app/components/AppPopupShell.vue'
import { useAuthStore } from '@/auth/model/auth.store'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const authStore = useAuthStore()
const router = useRouter()
const firstActionButton = ref<HTMLButtonElement | null>(null)

const fullName = computed(() => {
  if (!authStore.user) {
    return 'Not signed in'
  }

  return `${authStore.user.firstName} ${authStore.user.lastName}`
})

async function navigateTo(routeName: 'my-profile' | 'settings') {
  emit('close')
  await router.push({ name: routeName })
}

function onSupport() {
  emit('close')
}

async function onLogout() {
  emit('close')
  await authStore.logout()
  await router.push({ name: 'login' })
}

function focusFirstAction() {
  firstActionButton.value?.focus()
}

defineExpose({
  focusFirstAction,
})

onMounted(() => {
  focusFirstAction()
})
</script>

<template>
  <AppPopupShell id="profile-popup" min-width="280px" role="dialog" aria-label="User menu">
    <div class="profile-container">
      <div class="identity-section">
        <p class="user-name">{{ fullName }}</p>
        <p class="user-email">{{ authStore.user?.email ?? 'No email available' }}</p>
      </div>

      <div class="context-section">
        <div class="context-row">
          <span class="context-label">Organization</span>
          <span class="context-value context-value-secondary">
            {{ authStore.appContext?.organizationName ?? 'Unavailable' }}
          </span>
        </div>
        <div class="context-row">
          <span class="context-label">Establishment</span>
          <span class="context-value context-value-secondary">
            {{ authStore.appContext?.establishmentName ?? 'Unavailable' }}
          </span>
        </div>
      </div>
      <div class="actions-section">
        <button
          ref="firstActionButton"
          type="button"
          class="menu-action"
          @click="navigateTo('my-profile')"
        >
          <span class="menu-action-icon" aria-hidden="true">
            <svg viewBox="0 0 20 20">
              <path
                d="M10 10a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5Z"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
              />
              <path
                d="M4.75 16.5a5.25 5.25 0 0 1 10.5 0"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
              />
            </svg>
          </span>
          <span>My profile</span>
        </button>

        <button type="button" class="menu-action" @click="navigateTo('settings')">
          <span class="menu-action-icon" aria-hidden="true">
            <svg viewBox="0 0 20 20">
              <path
                d="M10 12.25a2.25 2.25 0 1 0 0-4.5 2.25 2.25 0 0 0 0 4.5Z"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
              />
              <path
                d="M16 10c0-.28-.03-.56-.08-.82l1.22-.96-1.2-2.08-1.52.47a5.92 5.92 0 0 0-1.42-.82l-.3-1.57H10.3L10 5.79c-.5.13-.97.33-1.42.58l-1.52-.47-1.2 2.08 1.22.96c-.05.26-.08.54-.08.82s.03.56.08.82l-1.22.96 1.2 2.08 1.52-.47c.45.25.92.45 1.42.58l.3 1.57h2.4l.3-1.57c.5-.13.97-.33 1.42-.58l1.52.47 1.2-2.08-1.22-.96c.05-.26.08-.54.08-.82Z"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
              />
            </svg>
          </span>
          <span>Settings</span>
        </button>

        <button type="button" class="menu-action" @click="onSupport">
          <span class="menu-action-icon" aria-hidden="true">
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

        <button type="button" class="menu-action menu-action-signout" @click="onLogout">
          <span class="menu-action-icon" aria-hidden="true">
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
  </AppPopupShell>
</template>

<style scoped>
.profile-container {
  display: flex;
  flex-direction: column;
}

.identity-section,
.context-section,
.actions-section {
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
}

.identity-section,
.context-section {
  gap: 6px;
  border-bottom: 1px solid var(--color-border-muted);
}

.actions-section {
  padding: 8px;
  gap: 2px;
}

.user-name,
.user-email,
.context-label,
.context-value {
  margin: 0;
}

.user-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.user-email,
.context-label {
  color: var(--color-text-secondary);
}

.user-email,
.context-label,
.context-value-secondary {
  font-size: 0.8125rem;
}

.context-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.context-value {
  color: var(--color-text-primary);
}

.menu-action {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-primary);
  font: inherit;
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
}

.menu-action:hover {
  background: var(--color-surface);
}

.menu-action-icon {
  display: inline-flex;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.menu-action-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
}

.menu-action-signout {
  color: var(--color-critical);
}
</style>
