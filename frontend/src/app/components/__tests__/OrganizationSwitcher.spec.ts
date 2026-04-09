import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

const authStoreMock = {
  isPlatformAdmin: true,
  appContext: {
    organizationId: 'org-1',
  },
  organizations: [] as Array<{ id: string; name: string }>,
  isLoadingOrganizations: false,
  updateSelectedOrganization: vi.fn(),
}

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

describe('OrganizationSwitcher', () => {
  beforeEach(() => {
    authStoreMock.isPlatformAdmin = true
    authStoreMock.appContext.organizationId = 'org-1'
    authStoreMock.organizations = []
    authStoreMock.isLoadingOrganizations = false
    authStoreMock.updateSelectedOrganization.mockReset()
  })

  it('does not render when the platform admin only has one organization', async () => {
    authStoreMock.organizations = [{ id: 'org-1', name: 'Only Organization' }]

    const { default: OrganizationSwitcher } = await import('@/app/components/OrganizationSwitcher.vue')
    const wrapper = mount(OrganizationSwitcher)

    expect(wrapper.find('.switcher').exists()).toBe(false)
  })

  it('renders when the platform admin has multiple organizations', async () => {
    authStoreMock.organizations = [
      { id: 'org-1', name: 'Alpha Group' },
      { id: 'org-2', name: 'Beta Group' },
    ]

    const { default: OrganizationSwitcher } = await import('@/app/components/OrganizationSwitcher.vue')
    const wrapper = mount(OrganizationSwitcher)

    expect(wrapper.find('.switcher').exists()).toBe(true)
    expect(wrapper.findAll('option')).toHaveLength(2)
  })
})
