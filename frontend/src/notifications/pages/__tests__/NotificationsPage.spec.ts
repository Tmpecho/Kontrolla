import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

const listNotificationsMock = vi.fn()
const markNotificationReadMock = vi.fn()
const markAllNotificationsReadMock = vi.fn()
const refreshUnreadCountMock = vi.fn()
const setUnreadCountMock = vi.fn()

vi.mock('@/notifications/api/notifications.api', () => ({
  listNotifications: listNotificationsMock,
  markNotificationRead: markNotificationReadMock,
  markAllNotificationsRead: markAllNotificationsReadMock,
}))

vi.mock('@/notifications/model/notifications.store', () => ({
  useNotificationsStore: () => ({
    unreadCount: 2,
    refreshUnreadCount: refreshUnreadCountMock,
    setUnreadCount: setUnreadCountMock,
  }),
}))

describe('NotificationsPage', () => {
  beforeEach(() => {
    listNotificationsMock.mockReset()
    markNotificationReadMock.mockReset()
    markAllNotificationsReadMock.mockReset()
    refreshUnreadCountMock.mockReset()
    setUnreadCountMock.mockReset()
    refreshUnreadCountMock.mockResolvedValue(2)
  })

  it('loads notifications and marks an unread item as read', async () => {
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
      size: 20,
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
      routes: [{ path: '/', name: 'notifications', component: { template: '<div />' } }],
    })
    router.push({ name: 'notifications' })
    await router.isReady()

    const { default: NotificationsPage } = await import('@/notifications/pages/NotificationsPage.vue')
    const wrapper = mount(NotificationsPage, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })

    await vi.waitFor(() => {
      expect(listNotificationsMock).toHaveBeenCalledTimes(1)
    })

    await vi.waitFor(() => {
      expect(wrapper.find('.notification-read-button').exists()).toBe(true)
    })

    await wrapper.get('.notification-read-button').trigger('click')

    expect(markNotificationReadMock).toHaveBeenCalledWith('notification-1')
    expect(setUnreadCountMock).toHaveBeenCalledWith(1)
  })

  it('switches to the unread filter and calls the API with the unread status', async () => {
    listNotificationsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', name: 'notifications', component: { template: '<div />' } }],
    })
    router.push({ name: 'notifications' })
    await router.isReady()

    const { default: NotificationsPage } = await import('@/notifications/pages/NotificationsPage.vue')
    const wrapper = mount(NotificationsPage, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })

    await vi.waitFor(() => {
      expect(listNotificationsMock).toHaveBeenCalledWith({ status: 'ALL', page: 0, size: 20 })
    })

    await wrapper.get('.filter-chip:last-child').trigger('click')

    expect(listNotificationsMock).toHaveBeenLastCalledWith({ status: 'UNREAD', page: 0, size: 20 })
  })
})
