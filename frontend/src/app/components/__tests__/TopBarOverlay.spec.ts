import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SUPPORT_MAILTO } from '@/shared/config/support'

const {
  authStoreMock,
  listNotificationsMock,
  markNotificationReadMock,
  notificationsStoreMock,
} = vi.hoisted(() => ({
  authStoreMock: {
    user: {
      email: 'user@example.com',
      firstName: 'Test',
      globalRoles: [] as string[],
      lastName: 'User',
    },
    appContext: {
      organizationName: 'Kontrolla Demo',
      organizationRole: 'ORG_ADMIN' as const,
      establishmentName: 'Restaurant',
    },
    logout: vi.fn().mockResolvedValue(undefined),
  },
  listNotificationsMock: vi.fn(),
  markNotificationReadMock: vi.fn(),
  notificationsStoreMock: {
    unreadCount: 2,
    refreshUnreadCount: vi.fn().mockResolvedValue(2),
    setUnreadCount: vi.fn(),
  },
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/notifications/api/notifications.api', () => ({
  listNotifications: listNotificationsMock,
  markNotificationRead: markNotificationReadMock,
}))

vi.mock('@/notifications/model/notifications.store', () => ({
  useNotificationsStore: () => notificationsStoreMock,
}))

vi.mock('@/app/components/EstablishmentSwitcher.vue', () => ({
  default: {
    props: ['variant'],
    template: '<div class="establishment-switcher-stub" :data-variant="variant" />',
  },
}))

vi.mock('@/app/components/OrganizationSwitcher.vue', () => ({
  default: {
    props: ['variant'],
    template: '<div class="organization-switcher-stub" :data-variant="variant" />',
  },
}))

function mountTopBar() {
  return mountAsyncComponent()
}

async function mountAsyncComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'workspace-home', component: { template: '<div />' } },
      { path: '/ik-mat', name: 'ik-mat-dashboard', component: { template: '<div />' } },
      { path: '/ik-alkohol', name: 'ik-alkohol-dashboard', component: { template: '<div />' } },
      { path: '/admin/members', name: 'organization-members', component: { template: '<div />' } },
      { path: '/notifications', name: 'notifications', component: { template: '<div />' } },
      { path: '/account/profile', name: 'my-profile', component: { template: '<div />' } },
      { path: '/account/settings', name: 'settings', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
  router.push({ name: 'workspace-home' })
  await router.isReady()

  const { default: TopBar } = await import('@/app/components/TopBar.vue')
  return mount(TopBar, {
    attachTo: document.body,
    global: {
      plugins: [router],
    },
  })
}

describe('TopBar overlay integration', () => {
  beforeEach(() => {
    listNotificationsMock.mockReset()
    markNotificationReadMock.mockReset()
    notificationsStoreMock.refreshUnreadCount.mockClear()
    notificationsStoreMock.setUnreadCount.mockClear()
    notificationsStoreMock.unreadCount = 2
    authStoreMock.logout.mockClear()
    listNotificationsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('opens and closes the notifications popup with keyboard dismissal', async () => {
    const wrapper = await mountTopBar()

    const notificationsTrigger = wrapper.get('#notifications-trigger')
    await notificationsTrigger.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('No notifications')
    expect(notificationsTrigger.attributes('aria-controls')).toBe('notifications-popup')
    expect(document.body.querySelector('#notifications-popup')).not.toBeNull()

    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Escape' }))
    await flushPromises()

    expect(document.body.textContent).not.toContain('No notifications')
  })

  it('opens and closes the profile popup from the trigger button', async () => {
    const wrapper = await mountTopBar()

    const profileTrigger = wrapper.get('#profile-trigger')
    await profileTrigger.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('My profile')
    expect(document.body.textContent).toContain('Settings')
    expect(profileTrigger.attributes('aria-controls')).toBe('profile-popup')
    expect(document.body.querySelector('#profile-popup')).not.toBeNull()

    await profileTrigger.trigger('click')
    await flushPromises()

    expect(document.body.textContent).not.toContain('My profile')
  })

  it('renders the profile popup support action as a mailto link', async () => {
    const wrapper = await mountTopBar()

    await wrapper.get('#profile-trigger').trigger('click')
    await flushPromises()

    const supportLink = document.body.querySelector('#profile-popup a[href]') as HTMLAnchorElement | null

    expect(supportLink).not.toBeNull()
    expect(supportLink?.getAttribute('href')).toBe(SUPPORT_MAILTO)
    expect(document.body.textContent).toContain('Support')
  })
})
