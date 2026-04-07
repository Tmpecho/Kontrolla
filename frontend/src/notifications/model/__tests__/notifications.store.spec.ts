import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const getUnreadNotificationCountMock = vi.fn()
const authStoreMock = {
  isAuthenticated: true,
}

vi.mock('@/notifications/api/notifications.api', () => ({
  getUnreadNotificationCount: getUnreadNotificationCountMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

describe('notifications.store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    getUnreadNotificationCountMock.mockReset()
    authStoreMock.isAuthenticated = true
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('refreshes the unread count for authenticated users', async () => {
    getUnreadNotificationCountMock.mockResolvedValue(3)

    const { useNotificationsStore } = await import('@/notifications/model/notifications.store')
    const notificationsStore = useNotificationsStore()

    await notificationsStore.refreshUnreadCount()

    expect(notificationsStore.unreadCount).toBe(3)
    expect(notificationsStore.hasLoadedUnreadCount).toBe(true)
  })

  it('starts polling and updates the unread count on the interval', async () => {
    getUnreadNotificationCountMock.mockResolvedValue(5)

    const { useNotificationsStore } = await import('@/notifications/model/notifications.store')
    const notificationsStore = useNotificationsStore()

    notificationsStore.startPolling()
    await vi.runOnlyPendingTimersAsync()

    expect(getUnreadNotificationCountMock).toHaveBeenCalledTimes(2)
    expect(notificationsStore.unreadCount).toBe(5)

    notificationsStore.stopPolling()
  })

  it('clears the unread state when the user is not authenticated', async () => {
    authStoreMock.isAuthenticated = false

    const { useNotificationsStore } = await import('@/notifications/model/notifications.store')
    const notificationsStore = useNotificationsStore()
    notificationsStore.setUnreadCount(4)

    await notificationsStore.refreshUnreadCount()

    expect(notificationsStore.unreadCount).toBe(0)
    expect(notificationsStore.hasLoadedUnreadCount).toBe(false)
  })
})
