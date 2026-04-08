<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/notifications/api/notifications.api'
import { useNotificationsStore } from '@/notifications/model/notifications.store'
import type { NotificationItem, NotificationStatusFilter } from '@/notifications/model/notification.types'
import { formatNotificationTypeLabel, toNotificationRoute } from '@/notifications/model/notification.utils'
import { ApiError } from '@/shared/api/http'

const notificationDateTimeFormatter = new Intl.DateTimeFormat('nb-NO', {
  dateStyle: 'medium',
  timeStyle: 'short',
})
const notificationsStore = useNotificationsStore()
const notifications = ref<NotificationItem[]>([])
const isLoading = ref(false)
const isLoadingMore = ref(false)
const isMarkingAllRead = ref(false)
const errorMessage = ref<string | null>(null)
const actionErrorMessage = ref<string | null>(null)
const activeFilter = ref<NotificationStatusFilter>('ALL')
const currentPage = ref(0)
const totalPages = ref(0)

const hasNotifications = computed(() => notifications.value.length > 0)
const hasMoreNotifications = computed(() => currentPage.value + 1 < totalPages.value)

async function loadNotificationsPage(reset = false) {
  if (reset) {
    isLoading.value = true
    errorMessage.value = null
    actionErrorMessage.value = null
  } else {
    isLoadingMore.value = true
  }

  try {
    const nextPage = reset ? 0 : currentPage.value + 1
    const page = await listNotifications({
      status: activeFilter.value,
      page: nextPage,
      size: 20,
    })

    notifications.value = reset ? page.items : [...notifications.value, ...page.items]
    currentPage.value = page.page
    totalPages.value = page.totalPages
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Notifications are temporarily unavailable.'
  } finally {
    isLoading.value = false
    isLoadingMore.value = false
  }
}

async function selectFilter(filter: NotificationStatusFilter) {
  if (filter === activeFilter.value) {
    return
  }

  activeFilter.value = filter
  await loadNotificationsPage(true)
}

async function handleMarkRead(notification: NotificationItem) {
  actionErrorMessage.value = null

  try {
    const updatedNotification = await markNotificationRead(notification.id)

    if (activeFilter.value === 'UNREAD') {
      notifications.value = notifications.value.filter((item) => item.id !== notification.id)
    } else {
      notifications.value = notifications.value.map((item) =>
        item.id === notification.id ? updatedNotification : item,
      )
    }

    notificationsStore.setUnreadCount(Math.max(0, notificationsStore.unreadCount - 1))
  } catch (error) {
    actionErrorMessage.value =
      error instanceof ApiError ? error.message : 'Unable to mark this notification as read.'
  }
}

async function handleMarkAllRead() {
  isMarkingAllRead.value = true
  actionErrorMessage.value = null

  try {
    await markAllNotificationsRead()
    notifications.value =
      activeFilter.value === 'UNREAD'
        ? []
        : notifications.value.map((notification) => ({
            ...notification,
            isUnread: false,
            readAt: notification.readAt ?? new Date().toISOString(),
          }))
    notificationsStore.setUnreadCount(0)
  } catch (error) {
    actionErrorMessage.value =
      error instanceof ApiError ? error.message : 'Unable to mark all notifications as read.'
  } finally {
    isMarkingAllRead.value = false
  }
}

function formatDateTime(createdAt: string): string {
  return notificationDateTimeFormatter.format(new Date(createdAt))
}

onMounted(async () => {
  await loadNotificationsPage(true)
  await notificationsStore.refreshUnreadCount().catch(() => undefined)
})
</script>

<template>
  <div class="notifications-page">
    <header class="notifications-header">
      <div>
        <h1>Notifications</h1>
        <p>Recent checklist and deviation activity that needs follow-up.</p>
      </div>

      <button
        type="button"
        class="mark-all-button"
        :disabled="notificationsStore.unreadCount === 0 || isMarkingAllRead"
        @click="handleMarkAllRead"
      >
        Mark all as read
      </button>
    </header>

    <section class="filter-bar" aria-label="Notification filters">
      <button
        type="button"
        class="filter-chip"
        :class="{ 'filter-chip-active': activeFilter === 'ALL' }"
        @click="selectFilter('ALL')"
      >
        All
      </button>
      <button
        type="button"
        class="filter-chip"
        :class="{ 'filter-chip-active': activeFilter === 'UNREAD' }"
        @click="selectFilter('UNREAD')"
      >
        Unread
      </button>
    </section>

    <div v-if="isLoading" class="state-card">
      <p>Loading notifications...</p>
    </div>

    <div v-else-if="errorMessage" class="state-card state-card-error">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else class="notifications-surface">
      <div v-if="actionErrorMessage" class="state-card state-card-error">
        <p>{{ actionErrorMessage }}</p>
      </div>

      <ul v-if="hasNotifications" class="notifications-list">
        <li v-for="notification in notifications" :key="notification.id" class="notifications-list-item">
          <article
            class="notification-row"
            :class="{
              'notification-row-unread': notification.isUnread,
              'notification-row-read': !notification.isUnread,
            }"
          >
            <div class="notification-copy">
              <div class="notification-meta">
                <span class="notification-type">{{ formatNotificationTypeLabel(notification.type) }}</span>
                <time :datetime="notification.createdAt">{{ formatDateTime(notification.createdAt) }}</time>
              </div>
              <h2 class="notification-title">{{ notification.title }}</h2>
              <p class="notification-message">{{ notification.message }}</p>
            </div>

            <div class="notification-actions">
              <RouterLink :to="toNotificationRoute(notification)" class="notification-open-link">
                Open
              </RouterLink>
              <button
                v-if="notification.isUnread"
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

      <p v-else class="notifications-empty-state">
        {{ activeFilter === 'UNREAD' ? 'No unread notifications.' : 'No notifications yet.' }}
      </p>
    </div>

    <button
      v-if="hasMoreNotifications"
      type="button"
      class="load-more-button"
      :disabled="isLoadingMore"
      @click="loadNotificationsPage()"
    >
      {{ isLoadingMore ? 'Loading more...' : 'Load more' }}
    </button>
  </div>
</template>

<style scoped>
.notifications-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.notifications-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.notifications-header h1,
.notifications-header p {
  margin: 0;
}

.notifications-header p {
  margin-top: 8px;
  color: var(--color-text-secondary);
}

.mark-all-button,
.load-more-button,
.notification-read-button {
  padding: 8px 12px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  cursor: pointer;
}

.mark-all-button:disabled,
.load-more-button:disabled,
.notification-read-button:disabled {
  cursor: default;
  opacity: 0.6;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-chip {
  padding: 8px 12px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font: inherit;
  cursor: pointer;
}

.filter-chip-active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-white);
}

.notifications-surface,
.state-card {
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-white);
}

.state-card {
  padding: 20px;
}

.state-card p {
  margin: 0;
}

.state-card-error {
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

.notification-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.notification-row-unread {
  box-shadow: inset 3px 0 0 var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-white));
}

.notification-row-read {
  background: color-mix(in srgb, var(--color-text-secondary) 4%, var(--color-white));
}

.notification-copy {
  min-width: 0;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
}

.notification-type {
  font-weight: 600;
}

.notification-title,
.notification-message {
  margin: 0;
}

.notification-title {
  margin-top: 10px;
  font-size: 1rem;
}

.notification-message {
  margin-top: 6px;
  color: var(--color-text-secondary);
}

.notification-row-read .notification-title {
  color: color-mix(in srgb, var(--color-text-primary) 70%, var(--color-white));
}

.notification-row-read .notification-message,
.notification-row-read .notification-meta {
  color: color-mix(in srgb, var(--color-text-secondary) 75%, var(--color-white));
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.notification-open-link {
  padding: 8px 12px;
  border-radius: 4px;
  color: var(--color-primary);
  text-decoration: none;
}

.notifications-empty-state {
  margin: 0;
  padding: 24px 20px;
  color: var(--color-text-secondary);
}

.load-more-button {
  align-self: flex-start;
}

@media (max-width: 960px) {
  .notifications-header,
  .notification-row {
    flex-direction: column;
  }

  .notification-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
