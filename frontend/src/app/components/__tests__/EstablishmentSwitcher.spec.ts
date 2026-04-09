import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

const authStoreMock = {
  appContext: {
    organizationId: 'org-1',
    establishmentId: null as string | null,
  },
  establishments: [] as Array<{ id: string; name: string }>,
  isLoadingEstablishments: false,
  updateSelectedEstablishment: vi.fn(),
}

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

describe('EstablishmentSwitcher', () => {
  beforeEach(() => {
    authStoreMock.appContext.organizationId = 'org-1'
    authStoreMock.appContext.establishmentId = null
    authStoreMock.establishments = []
    authStoreMock.isLoadingEstablishments = false
    authStoreMock.updateSelectedEstablishment.mockReset()
  })

  it('does not render when the user only has one establishment', async () => {
    authStoreMock.establishments = [{ id: 'est-1', name: 'Only Establishment' }]

    const { default: EstablishmentSwitcher } = await import('@/app/components/EstablishmentSwitcher.vue')
    const wrapper = mount(EstablishmentSwitcher)

    expect(wrapper.find('.switcher').exists()).toBe(false)
  })

  it('renders when the user has multiple establishments', async () => {
    authStoreMock.establishments = [
      { id: 'est-1', name: 'First Establishment' },
      { id: 'est-2', name: 'Second Establishment' },
    ]

    const { default: EstablishmentSwitcher } = await import('@/app/components/EstablishmentSwitcher.vue')
    const wrapper = mount(EstablishmentSwitcher)

    expect(wrapper.find('.switcher').exists()).toBe(true)
    expect(wrapper.findAll('option')).toHaveLength(3)
  })

  it('keeps the compact variant label visually hidden to avoid stretching the top bar', async () => {
    authStoreMock.establishments = [
      { id: 'est-1', name: 'First Establishment' },
      { id: 'est-2', name: 'Second Establishment' },
    ]

    const { default: EstablishmentSwitcher } = await import('@/app/components/EstablishmentSwitcher.vue')
    const wrapper = mount(EstablishmentSwitcher, {
      props: {
        variant: 'compact',
      },
    })

    expect(wrapper.get('.switcher-label').classes()).toContain('switcher-label-sr-only')
    expect(wrapper.get('.switcher').classes()).toContain('switcher-compact')
  })
})
