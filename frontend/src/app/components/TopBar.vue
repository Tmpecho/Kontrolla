<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import NotificationsPopup from '@/app/components/NotificationsPopup.vue'
import SettingsPopup from '@/app/components/SettingsPopup.vue'
import ProfilePopup from '@/app/components/ProfilePopup.vue'

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
    <div class="top-bar-container" ref="popupArea">
        <div class="group-container">
            <h1 class="app-title">
                <RouterLink :to="{ name: 'workspace-home' }">Kontrolla</RouterLink>
            </h1>
        </div>

        <div class="group-container">
            <p>IK-Mat</p>
            <p>IK-Alkohol</p>
        </div>

        <div class="group-container icons-container">
            <div class="icon-wrapper">
                <img
                    class="top-bar-img"
                    src="@/assets/icons/notification.png"
                    alt="Notifications"
                    @click.stop="togglePopup('notifications')"
                />
                <NotificationsPopup v-if="activePopup === 'notifications'" />
            </div>

            <div class="icon-wrapper">
                <img
                    class="top-bar-img"
                    src="@/assets/icons/settings.png"
                    alt="Settings"
                    @click.stop="togglePopup('settings')"
                />
                <SettingsPopup v-if="activePopup === 'settings'" />
            </div>

            <div class="icon-wrapper">
                <img
                    class="top-bar-img"
                    src="@/assets/icons/profile.png"
                    alt="Profile"
                    @click.stop="togglePopup('profile')"
                />
                <ProfilePopup v-if="activePopup === 'profile'" />
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
    padding: 0px 30px;
    background-color: #f8f9fa;
}

.group-container {
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
    margin: 0px;
}

.app-title :deep(a) {
    font-size: 20px;
    color: #4a5568;
    text-decoration: none;
}

.top-bar-img {
    width: 20px;
    height: 20px;
    cursor: pointer;
}
</style>