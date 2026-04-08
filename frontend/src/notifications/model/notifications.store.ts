import { defineStore } from 'pinia'
import { ref } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import { getUnreadNotificationCount } from '@/notifications/api/notifications.api'

const POLL_INTERVAL_MS = 60_000

export const useNotificationsStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const isRefreshing = ref(false)
  const hasLoadedUnreadCount = ref(false)
  let pollHandle: number | null = null

  async function refreshUnreadCount() {
    const authStore = useAuthStore()

    if (!authStore.isAuthenticated) {
      unreadCount.value = 0
      hasLoadedUnreadCount.value = false
      return 0
    }

    isRefreshing.value = true

    try {
      const nextUnreadCount = await getUnreadNotificationCount()
      unreadCount.value = nextUnreadCount
      hasLoadedUnreadCount.value = true
      return nextUnreadCount
    } finally {
      isRefreshing.value = false
    }
  }

  function setUnreadCount(value: number) {
    unreadCount.value = Math.max(0, value)
    hasLoadedUnreadCount.value = true
  }

  function startPolling() {
    const authStore = useAuthStore()

    stopPolling()

    if (!authStore.isAuthenticated) {
      unreadCount.value = 0
      hasLoadedUnreadCount.value = false
      return
    }

    void refreshUnreadCount().catch(() => undefined)
    pollHandle = window.setInterval(() => {
      void refreshUnreadCount().catch(() => undefined)
    }, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (pollHandle !== null) {
      window.clearInterval(pollHandle)
      pollHandle = null
    }
  }

  function reset() {
    stopPolling()
    unreadCount.value = 0
    hasLoadedUnreadCount.value = false
    isRefreshing.value = false
  }

  return {
    unreadCount,
    isRefreshing,
    hasLoadedUnreadCount,
    refreshUnreadCount,
    setUnreadCount,
    startPolling,
    stopPolling,
    reset,
  }
})
