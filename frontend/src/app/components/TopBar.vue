<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import NotificationsPopup from '@/app/components/NotificationsPopup.vue'
import ProfilePopup from '@/app/components/ProfilePopup.vue'

const activePopup = ref<null | 'notifications' | 'profile'>(null)
const popupArea = ref<HTMLElement | null>(null)
const route = useRoute()

function togglePopup(type: 'notifications' | 'profile') {
  activePopup.value = activePopup.value === type ? null : type
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

      <RouterLink class="nav-link" :to="{name: 'ik-mat-dashboard' }">IK-Mat</RouterLink>
      <RouterLink class="nav-link" :to="{name: 'ik-alkohol-dashboard' }">IK-Alkohol</RouterLink>
    </div>

    <div ref="popupArea" class="right-container icons-container">
      <div class="icon-wrapper">
        <img
          alt="Notifications"
          class="top-bar-img"
          src="@/assets/icons/notification.png"
          @click.stop="togglePopup('notifications')"
        />
        <NotificationsPopup v-if="activePopup === 'notifications'" />
      </div>

      <div class="icon-wrapper icon-wrapper-profile">
        <img
          alt="Profile"
          class="top-bar-img"
          src="@/assets/icons/profile.png"
          @click.stop="togglePopup('profile')"
        />
        <ProfilePopup v-if="activePopup === 'profile'" @close="closePopup" />
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

.nav-link.router-link-active {
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
