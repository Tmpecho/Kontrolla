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
    const updatedNotification = await markNotificationRead(notification.id)
    notifications.value = notifications.value.map((item) =>
      item.id === notification.id ? updatedNotification : item,
    )
    notificationsStore.setUnreadCount(Math.max(0, notificationsStore.unreadCount - 1))
  }

  emit('close')
  await router.push(toNotificationRoute(notification))
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
            <button
              type="button"
              class="notification-link"
              :class="{ 'notification-link-unread': notification.isUnread }"
              @click="openNotification(notification)"
            >
              <div class="notification-meta">
                <span>{{ formatNotificationTypeLabel(notification.type) }}</span>
                <time :datetime="notification.createdAt">
                  {{
                    new Intl.DateTimeFormat('nb-NO', { dateStyle: 'short', timeStyle: 'short' }).format(
                      new Date(notification.createdAt),
                    )
                  }}
                </time>
              </div>
              <strong class="notification-title">{{ notification.title }}</strong>
              <span class="notification-message">{{ notification.message }}</span>
            </button>
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
  color: var(--color-danger);
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

.notification-link-unread {
  box-shadow: inset 3px 0 0 var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-white));
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

.notifications-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-muted);
}

.view-all-link {
  color: var(--color-primary);
  text-decoration: none;
}
</style>
