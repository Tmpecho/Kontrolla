import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

const listNotificationsMock = vi.fn()
const markNotificationReadMock = vi.fn()
const refreshUnreadCountMock = vi.fn()
const setUnreadCountMock = vi.fn()

vi.mock('@/notifications/api/notifications.api', () => ({
  listNotifications: listNotificationsMock,
  markNotificationRead: markNotificationReadMock,
}))

vi.mock('@/notifications/model/notifications.store', () => ({
  useNotificationsStore: () => ({
    unreadCount: 3,
    refreshUnreadCount: refreshUnreadCountMock,
    setUnreadCount: setUnreadCountMock,
  }),
}))

describe('NotificationsPopup', () => {
  beforeEach(() => {
    listNotificationsMock.mockReset()
    markNotificationReadMock.mockReset()
    refreshUnreadCountMock.mockReset()
    refreshUnreadCountMock.mockResolvedValue(3)
    setUnreadCountMock.mockReset()
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('loads recent notifications and shows the view-all link', async () => {
    listNotificationsMock.mockResolvedValue({
      items: [
        {
          id: 'notification-1',
          recipientUserId: 'user-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          type: 'CHECKLIST_ASSIGNED',
          title: 'Morning shift',
          message: 'You were assigned this checklist run.',
          resourceType: 'CHECKLIST_RUN',
          resourceId: 'run-1',
          createdAt: '2026-04-07T08:00:00Z',
          readAt: null,
          isUnread: true,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/app/notifications', name: 'notifications', component: { template: '<div />' } }],
    })
    router.push({ name: 'notifications' })
    await router.isReady()

    const { default: NotificationsPopup } = await import('@/app/components/NotificationsPopup.vue')
    mount(NotificationsPopup, {
      attachTo: document.body,
      props: {
        open: true,
      },
      global: {
        plugins: [router],
      },
    })

    await vi.waitFor(() => {
      expect(document.body.textContent).toContain('Morning shift')
    })

    expect(document.body.querySelector('.view-all-link')?.getAttribute('href')).toBe(
      '/app/notifications',
    )
  })

  it('marks an unread notification as read from the popup', async () => {
    listNotificationsMock.mockResolvedValue({
      items: [
        {
          id: 'notification-1',
          recipientUserId: 'user-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          type: 'CHECKLIST_ASSIGNED',
          title: 'Morning shift',
          message: 'You were assigned this checklist run.',
          resourceType: 'CHECKLIST_RUN',
          resourceId: 'run-1',
          createdAt: '2026-04-07T08:00:00Z',
          readAt: null,
          isUnread: true,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })
    markNotificationReadMock.mockResolvedValue({
      id: 'notification-1',
      recipientUserId: 'user-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      type: 'CHECKLIST_ASSIGNED',
      title: 'Morning shift',
      message: 'You were assigned this checklist run.',
      resourceType: 'CHECKLIST_RUN',
      resourceId: 'run-1',
      createdAt: '2026-04-07T08:00:00Z',
      readAt: '2026-04-07T08:30:00Z',
      isUnread: false,
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/app/notifications', name: 'notifications', component: { template: '<div />' } }],
    })
    router.push({ name: 'notifications' })
    await router.isReady()

    const { default: NotificationsPopup } = await import('@/app/components/NotificationsPopup.vue')
    mount(NotificationsPopup, {
      attachTo: document.body,
      props: {
        open: true,
      },
      global: {
        plugins: [router],
      },
    })

    await vi.waitFor(() => {
      expect(document.body.querySelector('.notification-read-button')).not.toBeNull()
    })

    ;(document.body.querySelector('.notification-read-button') as HTMLButtonElement).click()

    await vi.waitFor(() => {
      expect(markNotificationReadMock).toHaveBeenCalledWith('notification-1')
      expect(setUnreadCountMock).toHaveBeenCalledWith(2)
      expect(document.body.querySelector('.notification-read-button')).toBeNull()
      expect(document.body.querySelector('.notification-link-read')).not.toBeNull()
    })
  })

  it('shows an inline error when marking a notification as read fails', async () => {
    listNotificationsMock.mockResolvedValue({
      items: [
        {
          id: 'notification-1',
          recipientUserId: 'user-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          type: 'CHECKLIST_ASSIGNED',
          title: 'Morning shift',
          message: 'You were assigned this checklist run.',
          resourceType: 'CHECKLIST_RUN',
          resourceId: 'run-1',
          createdAt: '2026-04-07T08:00:00Z',
          readAt: null,
          isUnread: true,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })
    markNotificationReadMock.mockRejectedValue(new Error('Request failed'))

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/app/notifications', name: 'notifications', component: { template: '<div />' } }],
    })
    router.push({ name: 'notifications' })
    await router.isReady()

    const { default: NotificationsPopup } = await import('@/app/components/NotificationsPopup.vue')
    mount(NotificationsPopup, {
      attachTo: document.body,
      props: {
        open: true,
      },
      global: {
        plugins: [router],
      },
    })

    await vi.waitFor(() => {
      expect(document.body.querySelector('.notification-read-button')).not.toBeNull()
    })

    ;(document.body.querySelector('.notification-read-button') as HTMLButtonElement).click()
    await flushPromises()

    expect(setUnreadCountMock).not.toHaveBeenCalled()
    expect(document.body.textContent).toContain('Unable to mark this notification as read.')
    expect(document.body.querySelector('.notification-read-button')).not.toBeNull()
  })
})
