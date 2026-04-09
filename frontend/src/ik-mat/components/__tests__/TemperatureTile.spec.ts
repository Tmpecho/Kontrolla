import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import TemperatureTile from '@/ik-mat/components/TemperatureTile.vue'
import { ApiError } from '@/shared/api/http'

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (error?: unknown) => void

  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

function createUnit(id: string, name: string) {
  return {
    id,
    name,
    location: 'Kitchen',
    type: 'FRIDGE' as const,
    dueByTime: '08:30:00',
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs: [
      {
        id: `${id}-log-1`,
        measuredAt: '2026-04-09T06:10:00Z',
        temperatureCelsius: 3.4,
        note: 'Opening check completed.',
        loggedByName: 'Jonas Berg',
      },
    ],
  }
}

const { authStoreMock, appEnvMock, listTemperatureUnitsMock } = vi.hoisted(() => {
  const { reactive } = require('vue') as typeof import('vue')

  return {
    authStoreMock: reactive({
      appContext: {
        organizationId: 'org-1',
        establishmentId: 'est-1',
        organizationRole: 'ORG_EMPLOYEE',
        organizationName: null,
        establishmentName: null,
      } as null | {
        organizationId: null | string
        establishmentId: null | string
        organizationRole: null | string
        organizationName: null | string
        establishmentName: null | string
      },
      requiresEstablishmentSelection: false,
    }),
    appEnvMock: {
      mode: 'test',
      isDevelopment: true,
      isProduction: false,
      apiBaseUrl: 'http://localhost:8080',
      defaultOrganizationId: undefined as string | undefined,
      defaultEstablishmentId: undefined as string | undefined,
      showDevLoginHint: false,
    },
    listTemperatureUnitsMock: vi.fn(),
  }
})

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/ik-mat/api/temperature.api', () => ({
  listTemperatureUnits: listTemperatureUnitsMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

function mountTile() {
  return mount(TemperatureTile, {
    props: {
      temperaturePageTo: { name: 'ik-mat-temperature' },
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

describe('TemperatureTile', () => {
  afterEach(() => {
    listTemperatureUnitsMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }
    authStoreMock.requiresEstablishmentSelection = false
    appEnvMock.isDevelopment = true
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined
  })

  it('passes a route location to the tile link', async () => {
    listTemperatureUnitsMock.mockResolvedValue([createUnit('unit-1', 'Sushi prep fridge')])

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.find('[data-target-name="ik-mat-temperature"]').exists()).toBe(true)
  })

  it('stops loading immediately when context becomes unavailable', async () => {
    const deferred = createDeferred<ReturnType<typeof createUnit>[]>()
    listTemperatureUnitsMock.mockReturnValueOnce(deferred.promise)

    const wrapper = mountTile()
    await nextTick()

    expect(wrapper.text()).toContain('Loading temperature units...')

    authStoreMock.appContext = {
      organizationId: null,
      establishmentId: null,
      organizationRole: null,
      organizationName: null,
      establishmentName: null,
    }
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Loading temperature units...')
    expect(wrapper.text()).toContain('load temperature units')

    deferred.resolve([createUnit('unit-1', 'Sushi prep fridge')])
    await flushPromises()

    expect(wrapper.text()).not.toContain('Sushi prep fridge')
  })

  it('ignores stale responses when a newer context load finishes later', async () => {
    const firstRequest = createDeferred<ReturnType<typeof createUnit>[]>()
    const secondRequest = createDeferred<ReturnType<typeof createUnit>[]>()

    listTemperatureUnitsMock.mockImplementation(({ organizationId }: { organizationId: string }) => {
      return organizationId === 'org-1' ? firstRequest.promise : secondRequest.promise
    })

    const wrapper = mountTile()
    await nextTick()

    authStoreMock.appContext = {
      organizationId: 'org-2',
      establishmentId: 'est-2',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }
    await nextTick()

    secondRequest.resolve([createUnit('unit-2', 'Dessert freezer')])
    await flushPromises()
    expect(wrapper.text()).toContain('Dessert freezer')

    firstRequest.resolve([createUnit('unit-1', 'Sushi prep fridge')])
    await flushPromises()

    expect(wrapper.text()).toContain('Dessert freezer')
    expect(wrapper.text()).not.toContain('Sushi prep fridge')
  })

  it('shows API errors from the latest request only', async () => {
    listTemperatureUnitsMock.mockRejectedValue(new ApiError('Could not load temperature units.', 500))

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load temperature units.')
  })
})
