<script lang="ts" setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import NotificationsPopup from '@/app/components/NotificationsPopup.vue'
import ProfilePopup from '@/app/components/ProfilePopup.vue'

const activePopup = ref<null | 'notifications' | 'profile'>(null)
const popupArea = ref<HTMLElement | null>(null)
const notificationsButton = ref<HTMLButtonElement | null>(null)
const profileButton = ref<HTMLButtonElement | null>(null)
const notificationsPopup = ref<InstanceType<typeof NotificationsPopup> | null>(null)
const profilePopup = ref<InstanceType<typeof ProfilePopup> | null>(null)
const route = useRoute()

function isServiceActive(section: 'ik-mat' | 'ik-alkohol') {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return routeName.startsWith(`${section}-`)
}

async function togglePopup(type: 'notifications' | 'profile') {
  if (activePopup.value === type) {
    closePopup()
    return
  }

  activePopup.value = type
  await nextTick()

  if (type === 'notifications') {
    notificationsPopup.value?.focusPopup()
    return
  }

  profilePopup.value?.focusFirstAction()
}

function closePopup() {
  activePopup.value = null
}

function handleClickOutside(event: MouseEvent) {
  if (!popupArea.value) return

  const target = event.target as Node
  if (!popupArea.value.contains(target)) {
    closePopup()
  }
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (activePopup.value === 'notifications') {
      notificationsButton.value?.focus()
    }

    if (activePopup.value === 'profile') {
      profileButton.value?.focus()
    }

    closePopup()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscape)
})

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
      <h1 class="app-title">
        <RouterLink :to="{ name: 'workspace-home' }">Kontrolla</RouterLink>
      </h1>

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
    </div>

    <div ref="popupArea" class="right-container icons-container">
      <div class="icon-wrapper">
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
        </button>
        <NotificationsPopup
          v-if="activePopup === 'notifications'"
          ref="notificationsPopup"
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
          ref="profilePopup"
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

.left-container {
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

.icons-container {
  position: relative;
}

.icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.icon-wrapper-profile {
  z-index: 1;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

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
</style>
