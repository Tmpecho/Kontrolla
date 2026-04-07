<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppPopupShell from '@/app/components/AppPopupShell.vue'
import { listNotifications, markNotificationRead } from '@/notifications/api/notifications.api'
import { useNotificationsStore } from '@/notifications/model/notifications.store'
import type { NotificationItem } from '@/notifications/model/notification.types'
import {
  formatNotificationTypeLabel,
  toNotificationRoute,
} from '@/notifications/model/notification.utils'

const popupRef = ref<InstanceType<typeof AppPopupShell> | null>(null)
const notifications = ref<NotificationItem[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const notificationsStore = useNotificationsStore()
const router = useRouter()

const emit = defineEmits<{
  (e: 'close'): void
}>()

function focusPopup() {
  const popupElement = popupRef.value?.$el as HTMLElement | undefined
  popupElement?.focus()
}

async function loadRecentNotifications() {
  isLoading.value = true
  errorMessage.value = null

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

async function openNotification(notification: NotificationItem) {
	if (notification.isUnread) {
		await handleMarkRead(notification)
	}

	emit('close')
	await router.push(toNotificationRoute(notification))
}

async function handleMarkRead(notification: NotificationItem) {
	const updatedNotification = await markNotificationRead(notification.id)
	notifications.value = notifications.value.map((item) =>
		item.id === notification.id ? updatedNotification : item,
	)
	notificationsStore.setUnreadCount(Math.max(0, notificationsStore.unreadCount - 1))
}

function closePopup() {
  emit('close')
}

defineExpose({
  focusPopup,
})

onMounted(() => {
  focusPopup()
  void loadRecentNotifications()
  void notificationsStore.refreshUnreadCount().catch(() => undefined)
})
</script>

<template>
  <AppPopupShell
    id="notifications-popup"
    ref="popupRef"
    aria-label="Notifications"
    role="dialog"
  >
    <div class="notifications-container">
      <p v-if="isLoading" class="notifications-state">Loading notifications...</p>
      <p v-else-if="errorMessage" class="notifications-state notifications-state-error">{{ errorMessage }}</p>
      <template v-else-if="notifications.length > 0">
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
                  <time :datetime="notification.createdAt">
                    {{
                      new Intl.DateTimeFormat('nb-NO', { dateStyle: 'short', timeStyle: 'short' }).format(
                        new Date(notification.createdAt),
                      )
                  }}
                  </time>
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
  </AppPopupShell>
</template>

<style scoped>
.notifications-container {
  min-width: 320px;
}

.notifications-state {
  margin: 0;
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
  padding: 14px 16px;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.notification-item {
  display: flex;
  flex-direction: column;
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
  display: flex;
  justify-content: flex-end;
  padding: 0 16px 14px;
}

.notification-read-button {
  padding: 6px 10px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-white);
  color: var(--color-text-secondary);
  font: inherit;
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
</style>
