<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listNotifications, markNotificationRead } from '@/notifications/api/notifications.api'
import { useNotificationsStore } from '@/notifications/model/notifications.store'
import type { NotificationItem } from '@/notifications/model/notification.types'
import {
  formatNotificationTypeLabel,
  toNotificationRoute,
} from '@/notifications/model/notification.utils'
import AppOverlay from '@/shared/components/overlay/AppOverlay.vue'

defineProps<{
  open: boolean
  anchorEl?: HTMLElement | null
}>()

const notificationDateTimeFormatter = new Intl.DateTimeFormat('nb-NO', {
  dateStyle: 'short',
  timeStyle: 'short',
})
const notifications = ref<NotificationItem[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const actionErrorMessage = ref<string | null>(null)
const isMobileViewport = ref(false)
const notificationsStore = useNotificationsStore()
const router = useRouter()

const emit = defineEmits<{
  (e: 'close'): void
}>()

async function loadRecentNotifications() {
  isLoading.value = true
  errorMessage.value = null
  actionErrorMessage.value = null

  try {
    const page = await listNotifications({
      size: 5,
    })
    notifications.value = page.items
  } catch {
    errorMessage.value = 'Notifications are temporarily unavailable.'
  } finally {
    isLoading.value = false
  }
}

function updateViewportMode() {
  isMobileViewport.value = window.innerWidth <= 720
}

async function openNotification(notification: NotificationItem) {
  actionErrorMessage.value = null

  if (notification.isUnread && !(await handleMarkRead(notification))) {
    return
  }

  emit('close')
  await router.push(toNotificationRoute(notification))
}

async function handleMarkRead(notification: NotificationItem): Promise<boolean> {
  actionErrorMessage.value = null

  try {
    const updatedNotification = await markNotificationRead(notification.id)
    notifications.value = notifications.value.map((item) =>
      item.id === notification.id ? updatedNotification : item,
    )
    notificationsStore.setUnreadCount(Math.max(0, notificationsStore.unreadCount - 1))
    return true
  } catch {
    actionErrorMessage.value = 'Unable to mark this notification as read.'
    return false
  }
}

function closePopup() {
  emit('close')
}

const overlayVariant = computed(() => (isMobileViewport.value ? 'sheet-bottom' : 'popover'))

function formatDateTime(createdAt: string): string {
  return notificationDateTimeFormatter.format(new Date(createdAt))
}

onMounted(() => {
  updateViewportMode()
  window.addEventListener('resize', updateViewportMode)
  void loadRecentNotifications()
  void notificationsStore.refreshUnreadCount().catch(() => undefined)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportMode)
})
</script>

<template>
  <AppOverlay
    :anchor-el="anchorEl"
    :open="open"
    aria-label="Notifications"
    panel-id="notifications-popup"
    :variant="overlayVariant"
    @close="closePopup"
  >
    <div class="notifications-container">
      <div class="notifications-header">
        <button type="button" class="notifications-back-button" @click="closePopup">
          <svg aria-hidden="true" class="notifications-back-icon" viewBox="0 0 20 20">
            <path
              d="M11.75 4.75 6.5 10l5.25 5.25"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </svg>
          <span>Back</span>
        </button>
        <p class="notifications-title">Notifications</p>
      </div>

      <p v-if="isLoading" class="notifications-state">Loading notifications...</p>
      <p v-else-if="errorMessage" class="notifications-state notifications-state-error">{{ errorMessage }}</p>
      <template v-else-if="notifications.length > 0">
        <p v-if="actionErrorMessage" class="notifications-state notifications-state-error">
          {{ actionErrorMessage }}
        </p>
        <ul class="notifications-list">
          <li
            v-for="notification in notifications"
            :key="notification.id"
            class="notifications-list-item"
          >
            <article class="notification-item" :class="{ 'notification-item-unread': notification.isUnread }">
              <button
                type="button"
                class="notification-link"
                :class="{ 'notification-link-read': !notification.isUnread }"
                @click="openNotification(notification)"
              >
                <span class="notification-meta">
                  <span>{{ formatNotificationTypeLabel(notification.type) }}</span>
                  <time :datetime="notification.createdAt">{{ formatDateTime(notification.createdAt) }}</time>
                </span>
                <strong class="notification-title">{{ notification.title }}</strong>
                <span class="notification-message">{{ notification.message }}</span>
              </button>

              <div v-if="notification.isUnread" class="notification-actions">
                <button
                  type="button"
                  class="notification-read-button"
                  @click="handleMarkRead(notification)"
                >
                  Mark read
                </button>
              </div>
            </article>
          </li>
        </ul>

        <div class="notifications-footer">
          <RouterLink :to="{ name: 'notifications' }" class="view-all-link" @click="closePopup">
            View all notifications
          </RouterLink>
        </div>
      </template>
      <p v-else class="notifications-state">No notifications</p>
    </div>
  </AppOverlay>
</template>

<style scoped>
.notifications-container {
  min-width: 320px;
}

.notifications-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-muted);
}

.notifications-title,
.notifications-state {
  margin: 0;
}

.notifications-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.notifications-back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}

.notifications-back-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.notifications-state {
  padding: 14px 16px;
}

.notifications-state-error {
  color: var(--color-critical);
}

.notifications-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notifications-list-item + .notifications-list-item {
  border-top: 1px solid var(--color-border-muted);
}

.notification-link {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px 46px;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.notification-item {
  display: flex;
  flex-direction: column;
  position: relative;
}

.notification-item-unread .notification-link {
  box-shadow: inset 3px 0 0 var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-white));
}

.notification-link-read {
  background: color-mix(in srgb, var(--color-text-secondary) 4%, var(--color-white));
}

.notification-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-text-secondary);
  font-size: 0.8rem;
}

.notification-title {
  font-size: 0.95rem;
}

.notification-message {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.notification-link-read .notification-title {
  color: color-mix(in srgb, var(--color-text-primary) 68%, var(--color-white));
}

.notification-link-read .notification-message,
.notification-link-read .notification-meta {
  color: color-mix(in srgb, var(--color-text-secondary) 75%, var(--color-white));
}

.notification-actions {
  position: absolute;
  right: 14px;
  bottom: 12px;
}

.notification-read-button {
  padding: 4px 8px;
  border: 1px solid color-mix(in srgb, var(--color-primary) 18%, var(--color-border-muted));
  border-radius: 4px;
  background: color-mix(in srgb, var(--color-white) 88%, transparent);
  color: color-mix(in srgb, var(--color-text-secondary) 88%, var(--color-text-primary));
  font: inherit;
  font-size: 0.8rem;
  line-height: 1.2;
  cursor: pointer;
}

.notifications-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-muted);
}

.view-all-link {
  color: var(--color-primary);
  text-decoration: none;
}

@media (max-width: 720px) {
  .notifications-container {
    min-width: 0;
    background-color: var(--color-container);
  }

  .notifications-header {
    padding: 16px;
  }
}
</style>
