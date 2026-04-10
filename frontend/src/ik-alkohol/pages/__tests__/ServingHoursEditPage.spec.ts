import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { AuthAppContext } from '@/auth/model/auth.types'
import ServingHoursEditPage from '@/ik-alkohol/pages/ServingHoursEditPage.vue'

const {
  listServingHoursMock,
  updateServingHoursMock,
  authStoreMock,
  appEnvMock,
  routerPushMock,
} = vi.hoisted(() => ({
  listServingHoursMock: vi.fn(),
  updateServingHoursMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
      organizationName: null,
      establishmentName: null,
    } as AuthAppContext | null,
    user: {
      globalRoles: [] as string[],
    },
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
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/establishments/api/serving-hours.api', () => ({
  listServingHours: listServingHoursMock,
  updateServingHours: updateServingHoursMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

function createWeek() {
  return [
    { dayOfWeek: 'MONDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
    { dayOfWeek: 'TUESDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
    { dayOfWeek: 'WEDNESDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
    { dayOfWeek: 'THURSDAY', closed: false, opensAt: '13:00:00', closesAt: '22:00:00' },
    { dayOfWeek: 'FRIDAY', closed: false, opensAt: '13:00:00', closesAt: '00:30:00' },
    { dayOfWeek: 'SATURDAY', closed: false, opensAt: '13:00:00', closesAt: '02:00:00' },
    { dayOfWeek: 'SUNDAY', closed: true, opensAt: null, closesAt: null },
  ] as const
}

describe('ServingHoursEditPage', () => {
  afterEach(() => {
    listServingHoursMock.mockReset()
    updateServingHoursMock.mockReset()
    routerPushMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
      organizationName: null,
      establishmentName: null,
    }
    authStoreMock.user.globalRoles = []
  })

  it('loads the serving hours form and saves updates', async () => {
    listServingHoursMock.mockResolvedValue(createWeek())
    updateServingHoursMock.mockResolvedValue(createWeek())

    const wrapper = mount(ServingHoursEditPage, {
      global: {
        stubs: {
          BaseButton: {
            template: '<button type="submit"><slot /></button>',
          },
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    const timeInputs = wrapper.findAll('input[type="time"]')
    await timeInputs[0]?.setValue('14:00')
    await timeInputs[1]?.setValue('23:00')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(updateServingHoursMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      days: expect.arrayContaining([
        expect.objectContaining({
          dayOfWeek: 'MONDAY',
          closed: false,
          opensAt: '14:00:00',
          closesAt: '23:00:00',
        }),
      ]),
    })
    expect(routerPushMock).toHaveBeenCalledWith({ name: 'ik-alkohol-dashboard' })
  })

  it('blocks employees from editing serving hours', async () => {
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }

    const wrapper = mount(ServingHoursEditPage, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Only organization managers and admins can edit serving hours.')
    expect(listServingHoursMock).not.toHaveBeenCalled()
  })
})
