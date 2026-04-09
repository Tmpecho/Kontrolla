import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { AuthAppContext } from '@/auth/model/auth.types'
import IKMatTemperaturePage from '@/ik-mat/pages/IKMatTemperaturePage.vue'
import { ApiError } from '@/shared/api/http'

function createUnit(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'unit-1',
    name: 'Sushi prep fridge',
    location: 'Hot kitchen',
    type: 'FRIDGE',
    dueByTime: '08:30:00',
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs: [
      {
        id: 'log-1',
        measuredAt: '2026-04-08T06:10:00Z',
        temperatureCelsius: 3.4,
        note: 'Morning opening check completed.',
        loggedByName: 'Jonas Berg',
      },
    ],
    ...overrides,
  }
}

const { listTemperatureUnitsMock, createTemperatureLogMock, authStoreMock, appEnvMock } = vi.hoisted(() => ({
  listTemperatureUnitsMock: vi.fn(),
  createTemperatureLogMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
    } as AuthAppContext | null,
    user: {
      id: 'user-1',
      email: 'maria@example.com',
      firstName: 'Maria',
      lastName: 'Nilsen',
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
}))

vi.mock('@/ik-mat/api/temperature.api', () => ({
  listTemperatureUnits: listTemperatureUnitsMock,
  createTemperatureLog: createTemperatureLogMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

function mountPage() {
  return mount(IKMatTemperaturePage, {
    global: {
      stubs: {
        TemperatureSparkline: {
          template: '<div class="sparkline-stub" />',
        },
        RouterLink: {
          props: ['to'],
          template: '<a :href="typeof to === \'string\' ? to : \'#\'"><slot /></a>',
        },
      },
    },
  })
}

describe('IKMatTemperaturePage', () => {
  afterEach(() => {
    listTemperatureUnitsMock.mockReset()
    createTemperatureLogMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }
    appEnvMock.isDevelopment = true
    appEnvMock.isProduction = false
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined
  })

  it('loads temperature units for the current context', async () => {
    listTemperatureUnitsMock.mockResolvedValue([createUnit()])

    const wrapper = mountPage()
    await flushPromises()

    expect(listTemperatureUnitsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
    })
    expect(wrapper.text()).toContain('Sushi prep fridge')
    expect(wrapper.text()).toContain('Logged by Jonas Berg')
  })

  it('shows a helpful message when organization context is missing', async () => {
    authStoreMock.appContext = {
      organizationId: null,
      establishmentId: null,
      organizationRole: null,
      organizationName: null,
      establishmentName: null,
    }

    const wrapper = mountPage()
    await flushPromises()

    expect(listTemperatureUnitsMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('load temperature units')
  })

  it('shows an error when loading temperature units fails', async () => {
    listTemperatureUnitsMock.mockRejectedValue(new ApiError('Could not load temperature units.', 500))

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load temperature units.')
  })

  it('creates a new temperature log and prepends it to the unit', async () => {
    listTemperatureUnitsMock.mockResolvedValue([createUnit()])
    createTemperatureLogMock.mockResolvedValue({
      id: 'log-2',
      measuredAt: '2026-04-09T06:10:00Z',
      temperatureCelsius: 3.2,
      note: 'Opening check completed.',
      loggedByName: 'Maria Nilsen',
    })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.get('.row-action').trigger('click')
    await wrapper.get('input[inputmode="decimal"]').setValue('3.2')
    await wrapper.get('input[type="datetime-local"]').setValue('2026-04-09T08:10')
    await wrapper.get('textarea').setValue('  Opening check completed.  ')
    await wrapper.get('.editor-button-primary').trigger('click')
    await flushPromises()

    expect(createTemperatureLogMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      temperatureUnitId: 'unit-1',
      temperatureCelsius: 3.2,
      measuredAt: expect.stringMatching(/^2026-04-09T/),
      note: 'Opening check completed.',
    })
    expect(wrapper.text()).toContain('Saved')
    expect(wrapper.text()).toContain('Logged by Maria Nilsen')
  })

describe('IKMatTemperaturePage mobile editor', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    window.innerWidth = 1024
  })

  it('opens the shared mobile sheet and closes it on escape', async () => {
    window.innerWidth = 700

    const { default: IKMatTemperaturePage } = await import('@/ik-mat/pages/IKMatTemperaturePage.vue')
    const wrapper = mount(IKMatTemperaturePage, {
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })

    const trigger = wrapper.find('button.row-action')
    ;(trigger.element as HTMLButtonElement).focus()
    await trigger.trigger('click')
    await flushPromises()

    const overlayPanel = document.body.querySelector('.app-overlay-panel')
    expect(overlayPanel).not.toBeNull()
    expect(overlayPanel?.getAttribute('aria-label')).toBe('Log temperature reading')

    document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'Escape' }))
    await flushPromises()

    expect(document.body.querySelector('.app-overlay-panel')).toBeNull()
    expect(document.activeElement).toBe(trigger.element)
  })
})

})
