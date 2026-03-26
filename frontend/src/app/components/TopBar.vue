<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import NotificationsPopup from '@/shared/components/NotificationsPopup.vue'
import SettingsPopup from '@/shared/components/SettingsPopup.vue'
import ProfilePopup from '@/shared/components/ProfilePopup.vue'

const activePopup = ref<null | 'notifications' | 'settings' | 'profile'>(null)
const popupArea = ref<HTMLElement | null>(null)

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

      <p class="appliance-text">IK-Mat</p>
      <p class="appliance-text">IK-Alkohol</p>
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
  padding: 20px 30px;
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
  color: var(--color-primary);
  font-size: 20px;
  text-decoration: none;
}

.appliance-text {
  margin: 0;
  color: var(--color-text-secondary);
}

.top-bar-img {
  width: 25px;
  height: 25px;
  cursor: pointer;
}
</style>
