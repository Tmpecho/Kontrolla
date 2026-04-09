<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import EstablishmentSwitcher from '@/app/components/EstablishmentSwitcher.vue'
import NotificationsPopup from '@/app/components/NotificationsPopup.vue'
import OrganizationSwitcher from '@/app/components/OrganizationSwitcher.vue'
import ProfilePopup from '@/app/components/ProfilePopup.vue'
import { useAuthStore } from '@/auth/model/auth.store'
import { useNotificationsStore } from '@/notifications/model/notifications.store'

const props = withDefaults(
  defineProps<{
    mobileNavOpen?: boolean
  }>(),
  {
    mobileNavOpen: false,
  },
)

const emit = defineEmits<{
  (e: 'toggle-mobile-nav'): void
}>()

const activePopup = ref<null | 'notifications' | 'profile'>(null)
const mobileNavButton = ref<HTMLButtonElement | null>(null)
const notificationsButton = ref<HTMLButtonElement | null>(null)
const profileButton = ref<HTMLButtonElement | null>(null)
const authStore = useAuthStore()
const route = useRoute()
const canManageMembers = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN'
  )
})

const notificationsStore = useNotificationsStore()
const unreadBadgeLabel = computed(() =>
  notificationsStore.unreadCount > 9 ? '9+' : String(notificationsStore.unreadCount),
)

const currentSectionLabel = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName === 'organization-members') {
    return 'Admin'
  }

  if (routeName === 'my-profile' || routeName === 'settings') {
    return 'Account'
  }

  if (routeName.startsWith('ik-mat-')) {
    return 'IK-mat'
  }

  if (routeName.startsWith('ik-alkohol-')) {
    return 'IK-alkohol'
  }

  return 'Workspace'
})

function isServiceActive(section: 'admin' | 'ik-mat' | 'ik-alkohol') {
  const routeName = typeof route.name === 'string' ? route.name : ''
  if (section === 'admin') {
    return routeName === 'organization-members'
  }

  return routeName.startsWith(`${section}-`)
}

function togglePopup(type: 'notifications' | 'profile') {
  if (activePopup.value === type) {
    closePopup()
    return
  }

  activePopup.value = type
}

function closePopup() {
  activePopup.value = null
}

watch(
  () => route.fullPath,
  () => {
    closePopup()
  },
)
</script>

<template>
  <div class="top-bar-container">
    <div class="left-container">
      <button
        id="mobile-nav-trigger"
        ref="mobileNavButton"
        type="button"
        class="mobile-menu-button"
        :aria-label="props.mobileNavOpen ? 'Close app navigation' : 'Open app navigation'"
        aria-haspopup="dialog"
        :aria-expanded="props.mobileNavOpen"
        aria-controls="mobile-navigation"
        @click="emit('toggle-mobile-nav')"
      >
        <svg aria-hidden="true" class="mobile-menu-icon" viewBox="0 0 20 20">
          <template v-if="props.mobileNavOpen">
            <path
              d="m5.5 5.5 9 9"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
            <path
              d="m14.5 5.5-9 9"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </template>
          <template v-else>
            <path
              d="M3.5 5.5h13"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
            <path
              d="M3.5 10h13"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
            <path
              d="M3.5 14.5h13"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </template>
        </svg>
      </button>

      <div class="brand-group">
        <h1 class="app-title">
          <RouterLink :to="{ name: 'workspace-home' }">Kontrolla</RouterLink>
        </h1>
        <p class="mobile-context-label">{{ currentSectionLabel }}</p>
      </div>

      <div class="desktop-service-links">
        <RouterLink
          class="nav-link"
          :data-active="isServiceActive('ik-mat')"
          :to="{ name: 'ik-mat-dashboard' }"
        >
          IK-Mat
        </RouterLink>
        <RouterLink
          class="nav-link"
          :data-active="isServiceActive('ik-alkohol')"
          :to="{ name: 'ik-alkohol-dashboard' }"
        >
          IK-Alkohol
        </RouterLink>
        <RouterLink
          v-if="canManageMembers"
          class="nav-link"
          :data-active="isServiceActive('admin')"
          :to="{ name: 'organization-members' }"
        >
          Admin
        </RouterLink>
      </div>
    </div>

    <OrganizationSwitcher class="mobile-organization-switcher" variant="panel" />
    <EstablishmentSwitcher class="mobile-establishment-switcher" variant="panel" />

    <div class="right-container icons-container">
      <OrganizationSwitcher class="desktop-organization-switcher" />
      <EstablishmentSwitcher class="desktop-establishment-switcher" />

      <div class="icon-wrapper icon-wrapper-notifications">
        <button
          id="notifications-trigger"
          ref="notificationsButton"
          type="button"
          class="icon-button"
          aria-label="Notifications"
          aria-haspopup="dialog"
          :aria-expanded="activePopup === 'notifications'"
          aria-controls="notifications-popup"
          @click.stop="togglePopup('notifications')"
        >
          <img alt="" class="top-bar-img" src="@/assets/icons/notification.png" />
          <span v-if="notificationsStore.unreadCount > 0" class="notification-badge">
            {{ unreadBadgeLabel }}
          </span>
        </button>
        <NotificationsPopup
          v-if="activePopup === 'notifications'"
          :anchor-el="notificationsButton"
          :open="activePopup === 'notifications'"
          @close="closePopup"
        />
      </div>

      <div class="icon-wrapper icon-wrapper-profile">
        <button
          id="profile-trigger"
          ref="profileButton"
          type="button"
          class="icon-button"
          aria-label="User menu"
          aria-haspopup="dialog"
          :aria-expanded="activePopup === 'profile'"
          aria-controls="profile-popup"
          @click.stop="togglePopup('profile')"
        >
          <img alt="" class="top-bar-img" src="@/assets/icons/profile.png" />
        </button>
        <ProfilePopup
          v-if="activePopup === 'profile'"
          :anchor-el="profileButton"
          :open="activePopup === 'profile'"
          @close="closePopup"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.top-bar-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 14px 30px;
  background-color: var(--color-white);
}

.mobile-establishment-switcher {
  display: none;
}

.mobile-organization-switcher {
  display: none;
}

.left-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.brand-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.desktop-service-links {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 50px;
}

.right-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 30px;
}

.desktop-establishment-switcher {
  width: 240px;
  flex-shrink: 0;
}

.desktop-organization-switcher {
  width: 240px;
  flex-shrink: 0;
}

.icons-container {
  position: relative;
}

.icon-wrapper {
  display: flex;
  align-items: center;
}

.mobile-menu-button,
.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.mobile-menu-button {
  display: none;
  width: 32px;
  height: 32px;
  color: var(--color-text-primary);
}

.mobile-menu-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.mobile-menu-button:focus-visible,
.icon-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 3px;
  border-radius: 4px;
}

.app-title {
  margin: 0;
  display: flex;
  align-items: center;
  line-height: 1;
}

.app-title :deep(a) {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 20px;
  text-decoration: none;
}

.mobile-context-label {
  display: none;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.nav-link {
  text-decoration: none;
  margin: 0;
  color: var(--color-text-secondary);
}

.nav-link[data-active='true'] {
  color: var(--color-text-primary);
  font-weight: 500;
}

.nav-link:hover {
  color: var(--color-text-primary);
}

.top-bar-img {
  width: 25px;
  height: 25px;
  cursor: pointer;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--color-danger);
  color: var(--color-white);
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

@media (max-width: 960px) {
  .top-bar-container {
    flex-wrap: wrap;
    align-items: flex-start;
    padding: 12px 16px;
    gap: 12px;
  }

  .mobile-menu-button,
  .mobile-context-label {
    display: inline-flex;
  }

  .desktop-service-links,
  .desktop-organization-switcher,
  .desktop-establishment-switcher,
  .icon-wrapper-notifications {
    display: none;
  }

  .mobile-organization-switcher,
  .mobile-establishment-switcher {
    display: flex;
    width: 100%;
    order: 3;
  }

  .left-container {
    gap: 12px;
    flex: 1;
  }

  .right-container {
    gap: 16px;
  }
}
</style>
