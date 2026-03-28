<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import NotificationsPopup from '@/app/components/NotificationsPopup.vue'
import SettingsPopup from '@/app/components/SettingsPopup.vue'
import ProfilePopup from '@/app/components/ProfilePopup.vue'

const route = useRoute()

const activePopup = ref<null | 'notifications' | 'settings' | 'profile'>(null)
const popupArea = ref<HTMLElement | null>(null)

const isIkMatActive = computed(() => {
  return typeof route.name === 'string' && route.name.startsWith('ik-mat-')
})

const isIkAlkoholActive = computed(() => {
  return typeof route.name === 'string' && route.name.startsWith('ik-alkohol-')
})

function togglePopup(type: 'notifications' | 'settings' | 'profile') {
  activePopup.value = activePopup.value === type ? null : type
}

function handleClickOutside(event: MouseEvent) {
  if (!popupArea.value) return

  const target = event.target as Node
  if (!popupArea.value.contains(target)) {
    activePopup.value = null
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="top-bar-container">
    <div class="left-container">
      <h1 class="app-title">
        <RouterLink :to="{ name: 'workspace-home' }">Kontrolla</RouterLink>
      </h1>

      <RouterLink
        class="nav-link"
        :class="{ 'nav-link-active': isIkMatActive }"
        :to="{ name: 'ik-mat-dashboard' }"
      >
        IK-Mat
      </RouterLink>

      <RouterLink
        class="nav-link"
        :class="{ 'nav-link-active': isIkAlkoholActive }"
        :to="{ name: 'ik-alkohol-dashboard' }"
      >
        IK-Alkohol
      </RouterLink>
    </div>

    <div class="right-container icons-container">
      <div class="icon-wrapper">
        <img
          alt="Notifications"
          class="top-bar-img"
          src="@/assets/icons/notification.png"
          @click.stop="togglePopup('notifications')"
        />
      </div>

      <div class="icon-wrapper">
        <img
          alt="Settings"
          class="top-bar-img"
          src="@/assets/icons/settings.png"
          @click.stop="togglePopup('settings')"
        />
      </div>

      <div class="icon-wrapper">
        <img
          alt="Profile"
          class="top-bar-img"
          src="@/assets/icons/profile.png"
          @click.stop="togglePopup('profile')"
        />
      </div>

      <div ref="popupArea" class="popup-wrapper">
        <ProfilePopup v-if="activePopup === 'profile'" />
        <SettingsPopup v-if="activePopup === 'settings'" />
        <NotificationsPopup v-if="activePopup === 'notifications'" />
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

.nav-link-active {
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
