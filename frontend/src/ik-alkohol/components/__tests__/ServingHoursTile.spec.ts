import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ServingHoursTile from '@/ik-alkohol/components/ServingHoursTile.vue'
import { ApiError } from '@/shared/api/http'

const mocks = vi.hoisted(() => ({
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_ADMIN',
      organizationName: null,
      establishmentName: null,
    },
    user: {
      globalRoles: [] as string[],
    },
    requiresEstablishmentSelection: false,
  },
  appEnvMock: {
    mode: 'test',
    isDevelopment: true,
    isProduction: false,
    apiBaseUrl: 'http://localhost:8080',
    defaultOrganizationId: undefined as string | undefined,
    defaultEstablishmentId: undefined as string | undefined,
    showDevLoginHint: false,
  },
  listServingHoursMock: vi.fn(),
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => mocks.authStoreMock,
}))

vi.mock('@/establishments/api/serving-hours.api', () => ({
  listServingHours: mocks.listServingHoursMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: mocks.appEnvMock,
}))

function mountTile() {
  return mount(ServingHoursTile, {
    props: {
      dashboardTo: { name: 'ik-alkohol-dashboard' },
      editTo: { name: 'ik-alkohol-serving-hours-edit' },
    },
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template:
            '<a :data-target-name="typeof to === \'string\' ? to : to?.name ?? \'\'"><slot /></a>',
        },
      },
    },
  })
}

describe('ServingHoursTile', () => {
  afterEach(() => {
    mocks.listServingHoursMock.mockReset()
    mocks.authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_ADMIN',
      organizationName: null,
      establishmentName: null,
    }
    mocks.authStoreMock.user.globalRoles = []
    mocks.authStoreMock.requiresEstablishmentSelection = false
  })

  it('shows the edit link for admins and renders loaded hours', async () => {
    mocks.listServingHoursMock.mockResolvedValue([
      { dayOfWeek: 'MONDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
      { dayOfWeek: 'TUESDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
      { dayOfWeek: 'WEDNESDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
      { dayOfWeek: 'THURSDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
      { dayOfWeek: 'FRIDAY', closed: false, opensAt: '13:00:00', closesAt: '00:30:00' },
      { dayOfWeek: 'SATURDAY', closed: false, opensAt: '13:00:00', closesAt: '02:00:00' },
      { dayOfWeek: 'SUNDAY', closed: true, opensAt: null, closesAt: null },
    ])

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.find('[data-target-name="ik-alkohol-serving-hours-edit"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Serving hours')
  })

  it('hides the edit link for employees', async () => {
    mocks.authStoreMock.appContext.organizationRole = 'ORG_EMPLOYEE'
    mocks.listServingHoursMock.mockResolvedValue([])

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.find('[data-target-name="ik-alkohol-serving-hours-edit"]').exists()).toBe(false)
  })

  it('shows API errors', async () => {
    mocks.listServingHoursMock.mockRejectedValue(new ApiError('Could not load serving hours.', 500))

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load serving hours.')
  })
})
