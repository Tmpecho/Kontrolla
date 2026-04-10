import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { AuthAppContext } from '@/auth/model/auth.types'
import TemperatureUnitCreatePage from '@/ik-mat/pages/TemperatureUnitCreatePage.vue'

const {
  createTemperatureUnitMock,
  authStoreMock,
  appEnvMock,
  routerPushMock,
} = vi.hoisted(() => ({
  createTemperatureUnitMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_ADMIN',
      organizationName: null,
      establishmentName: null,
    } as AuthAppContext | null,
    user: {
      id: 'user-1',
      email: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Larsen',
      globalRoles: [],
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

vi.mock('@/ik-mat/api/temperature.api', () => ({
  createTemperatureUnit: createTemperatureUnitMock,
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

describe('TemperatureUnitCreatePage', () => {
  afterEach(() => {
    createTemperatureUnitMock.mockReset()
    routerPushMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_ADMIN',
      organizationName: null,
      establishmentName: null,
    }
    authStoreMock.user.globalRoles = []
    appEnvMock.isDevelopment = true
    appEnvMock.isProduction = false
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined
  })

  it('submits a new temperature unit and routes back to the temperature page', async () => {
    createTemperatureUnitMock.mockResolvedValue({
      id: 'unit-1',
    })

    const wrapper = mount(TemperatureUnitCreatePage, {
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

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0]?.setValue(' Prep fridge ')
    await textInputs[1]?.setValue(' Main prep line ')
    await textInputs[2]?.setValue('2')
    await textInputs[3]?.setValue('4')

    await wrapper.get('select').setValue('FRIDGE')
    await wrapper.get('input[type="time"]').setValue('08:15')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createTemperatureUnitMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      name: 'Prep fridge',
      location: 'Main prep line',
      type: 'FRIDGE',
      dueByTime: '08:15:00',
      minimumTemperature: 2,
      maximumTemperature: 4,
    })
    expect(routerPushMock).toHaveBeenCalledWith({ name: 'ik-mat-temperature' })
  })

  it('shows a validation message when the range is invalid', async () => {
    const wrapper = mount(TemperatureUnitCreatePage, {
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

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0]?.setValue('Prep fridge')
    await textInputs[1]?.setValue('Main prep line')
    await textInputs[2]?.setValue('5')
    await textInputs[3]?.setValue('2')

    await wrapper.get('input[type="time"]').setValue('08:15')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Maximum temperature must be equal to or above the minimum.')
    expect(createTemperatureUnitMock).not.toHaveBeenCalled()
  })

  it('shows a blocked message for employees', async () => {
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }

    const wrapper = mount(TemperatureUnitCreatePage, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Only organization admins can create new temperature units.')
    expect(wrapper.text()).not.toContain('Create unit')
  })
})
