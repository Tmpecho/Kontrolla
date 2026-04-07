import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
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
    const wrapper = mount(NotificationsPopup, {
      attachTo: document.body,
      global: {
        plugins: [router],
      },
    })

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Morning shift')
    })

    expect(wrapper.get('.view-all-link').attributes('href')).toBe('/app/notifications')
  })
})
