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

const mocks = vi.hoisted(() => ({
  authStoreMock: null as null | {
    appContext: null | {
      organizationId: null | string
      establishmentId: null | string
      organizationRole: null | string
      organizationName: null | string
      establishmentName: null | string
    }
    requiresEstablishmentSelection: boolean
  },
  listTemperatureUnitsMock: vi.fn(),
}))

vi.mock('@/auth/model/auth.store', async () => {
  const { reactive } = await import('vue')

  if (!mocks.authStoreMock) {
    mocks.authStoreMock = reactive({
      appContext: {
        organizationId: 'org-1',
        establishmentId: 'est-1',
        organizationRole: 'ORG_EMPLOYEE',
        organizationName: null,
        establishmentName: null,
      },
      requiresEstablishmentSelection: false,
    })
  }

  return {
    useAuthStore: () => mocks.authStoreMock,
  }
})

vi.mock('@/ik-mat/api/temperature.api', () => ({
  listTemperatureUnits: mocks.listTemperatureUnitsMock,
}))

vi.mock('@/auth/model/workspace-context', async () => {
  const { computed } = await import('vue')

  return {
    useProtectedWorkspaceContext: () => ({
      organizationId: computed(() => mocks.authStoreMock?.appContext?.organizationId ?? null),
      establishmentId: computed(() => mocks.authStoreMock?.appContext?.establishmentId ?? null),
      availableEstablishmentIds: computed(() => {
        const establishmentId = mocks.authStoreMock?.appContext?.establishmentId ?? null
        return establishmentId ? [establishmentId] : []
      }),
      isStartupPending: computed(() => false),
      requiresEstablishmentSelection: computed(() => {
        return mocks.authStoreMock?.requiresEstablishmentSelection ?? false
      }),
      hasOrganizationContext: computed(() => Boolean(mocks.authStoreMock?.appContext?.organizationId)),
      hasEstablishmentContext: computed(() => {
        return Boolean(
          mocks.authStoreMock?.appContext?.organizationId &&
            mocks.authStoreMock?.appContext?.establishmentId,
        )
      }),
      hasAccessibleEstablishmentContext: computed(() => {
        return Boolean(
          mocks.authStoreMock?.appContext?.organizationId &&
            mocks.authStoreMock?.appContext?.establishmentId,
        )
      }),
    }),
  }
})

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
    mocks.listTemperatureUnitsMock.mockReset()
    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
      organizationName: null,
      establishmentName: null,
    }
    mocks.authStoreMock.requiresEstablishmentSelection = false
  })

  it('passes a route location to the tile link', async () => {
    mocks.listTemperatureUnitsMock.mockResolvedValue([createUnit('unit-1', 'Sushi prep fridge')])

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.find('[data-target-name="ik-mat-temperature"]').exists()).toBe(true)
  })

  it('stops loading immediately when context becomes unavailable', async () => {
    const deferred = createDeferred<ReturnType<typeof createUnit>[]>()
    mocks.listTemperatureUnitsMock.mockReturnValueOnce(deferred.promise)

    const wrapper = mountTile()
    await nextTick()

    expect(wrapper.text()).toContain('Loading temperature units...')

    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: null,
      establishmentId: null,
      organizationRole: null,
      organizationName: null,
      establishmentName: null,
    }
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Loading temperature units...')
    expect(wrapper.text()).toContain('Temperature logs are unavailable until organization context is ready.')

    deferred.resolve([createUnit('unit-1', 'Sushi prep fridge')])
    await flushPromises()

    expect(wrapper.text()).not.toContain('Sushi prep fridge')
  })

  it('ignores stale responses when a newer context load finishes later', async () => {
    const firstRequest = createDeferred<ReturnType<typeof createUnit>[]>()
    const secondRequest = createDeferred<ReturnType<typeof createUnit>[]>()

    mocks.listTemperatureUnitsMock.mockImplementation(({ organizationId }: { organizationId: string }) => {
      return organizationId === 'org-1' ? firstRequest.promise : secondRequest.promise
    })

    const wrapper = mountTile()
    await nextTick()

    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
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
    mocks.listTemperatureUnitsMock.mockRejectedValue(new ApiError('Could not load temperature units.', 500))

    const wrapper = mountTile()
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load temperature units.')
  })
})
