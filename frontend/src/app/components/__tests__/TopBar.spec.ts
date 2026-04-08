import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

const authStoreMock = {
  user: {
    globalRoles: [] as string[],
  },
  appContext: {
    organizationRole: 'ORG_ADMIN' as 'ORG_ADMIN',
  },
}

const notificationsStoreMock = {
  unreadCount: 0,
}

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/notifications/model/notifications.store', () => ({
  useNotificationsStore: () => notificationsStoreMock,
}))

vi.mock('@/app/components/NotificationsPopup.vue', () => ({
  default: {
    template: '<div />',
  },
}))

vi.mock('@/app/components/ProfilePopup.vue', () => ({
  default: {
    template: '<div />',
  },
}))

describe('TopBar', () => {
  beforeEach(() => {
    notificationsStoreMock.unreadCount = 0
  })

  it('caps the unread badge at 9+', async () => {
    notificationsStoreMock.unreadCount = 12

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'workspace-home', component: { template: '<div />' } },
        { path: '/ik-mat', name: 'ik-mat-dashboard', component: { template: '<div />' } },
        { path: '/ik-alkohol', name: 'ik-alkohol-dashboard', component: { template: '<div />' } },
        { path: '/admin/members', name: 'organization-members', component: { template: '<div />' } },
      ],
    })
    router.push({ name: 'workspace-home' })
    await router.isReady()

    const { default: TopBar } = await import('@/app/components/TopBar.vue')
    const wrapper = mount(TopBar, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.get('.notification-badge').text()).toBe('9+')
  })
})
