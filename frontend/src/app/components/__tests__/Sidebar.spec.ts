import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

const authStoreMock = {
  isSessionReady: true,
  isLoadingEstablishments: false,
  requiresEstablishmentSelection: false,
  user: {
    globalRoles: [] as string[],
  },
  appContext: {
    organizationName: 'Fjord Service Collective',
    establishmentName: 'Havglimt Restaurant',
    organizationRole: 'ORG_ADMIN' as const,
  },
  logout: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
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

describe('Sidebar', () => {
  beforeEach(() => {
    authStoreMock.logout.mockClear()
  })

  it('renders organization and establishment switchers inside the mobile sidebar', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'workspace-home', component: { template: '<div />' } },
        { path: '/notifications', name: 'notifications', component: { template: '<div />' } },
        { path: '/ik-mat', name: 'ik-mat-dashboard', component: { template: '<div />' } },
        { path: '/ik-mat/checklists', name: 'ik-mat-checklists', component: { template: '<div />' } },
        { path: '/ik-mat/temperature', name: 'ik-mat-temperature', component: { template: '<div />' } },
        { path: '/ik-mat/documents', name: 'ik-mat-documents', component: { template: '<div />' } },
        { path: '/ik-mat/deviation', name: 'ik-mat-deviation', component: { template: '<div />' } },
        { path: '/ik-alkohol', name: 'ik-alkohol-dashboard', component: { template: '<div />' } },
        { path: '/ik-alkohol/documents', name: 'ik-alkohol-documents', component: { template: '<div />' } },
        { path: '/ik-alkohol/deviation', name: 'ik-alkohol-deviation', component: { template: '<div />' } },
        { path: '/members', name: 'organization-members', component: { template: '<div />' } },
        { path: '/profile', name: 'my-profile', component: { template: '<div />' } },
        { path: '/settings', name: 'settings', component: { template: '<div />' } },
      ],
    })
    router.push({ name: 'workspace-home' })
    await router.isReady()

    const { default: Sidebar } = await import('@/app/components/Sidebar.vue')
    const wrapper = mount(Sidebar, {
      props: {
        variant: 'mobile',
      },
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.find('.mobile-switchers .organization-switcher-stub').attributes('data-variant')).toBe('panel')
    expect(wrapper.find('.mobile-switchers .establishment-switcher-stub').attributes('data-variant')).toBe('panel')
  })
})
