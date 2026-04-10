import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import IKMatDashboardPage from '@/ik-mat/pages/IKMatDashboardPage.vue'
import { ApiError } from '@/shared/api/http'

const { listChecklistRunsMock, authStoreMock } = vi.hoisted(() => ({
  listChecklistRunsMock: vi.fn(),
  authStoreMock: {
    isSessionReady: true,
    isStartupPending: false,
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
    } as
      | {
          organizationId: string | null
          establishmentId: string | null
        }
      | null,
    establishments: [] as Array<{
      id: string
      organizationId: string
      name: string
      type: 'RESTAURANT' | 'BAR' | 'CAFE' | 'OTHER'
      status: 'ACTIVE' | 'INACTIVE'
      createdAt: string
      updatedAt: string
    }>,
    requiresEstablishmentSelection: false,
  },
}))

vi.mock('@/checklists/api/checklist-runs.api', () => ({
  listChecklistRuns: listChecklistRunsMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/auth/model/workspace-context', async () => {
  const { computed } = await import('vue')

  return {
    useProtectedWorkspaceContext: () => ({
      organizationId: computed(() => authStoreMock.appContext?.organizationId ?? null),
      establishmentId: computed(() => authStoreMock.appContext?.establishmentId ?? null),
      availableEstablishmentIds: computed(() => {
        const selectedEstablishmentId = authStoreMock.appContext?.establishmentId ?? null
        if (selectedEstablishmentId) {
          return [selectedEstablishmentId]
        }

        return authStoreMock.establishments.map((establishment) => establishment.id)
      }),
      isStartupPending: computed(() => authStoreMock.isStartupPending),
      requiresEstablishmentSelection: computed(() => authStoreMock.requiresEstablishmentSelection),
      hasOrganizationContext: computed(() => Boolean(authStoreMock.appContext?.organizationId)),
      hasEstablishmentContext: computed(() => {
        return Boolean(
          authStoreMock.appContext?.organizationId && authStoreMock.appContext?.establishmentId,
        )
      }),
      hasAccessibleEstablishmentContext: computed(() => {
        const selectedEstablishmentId = authStoreMock.appContext?.establishmentId ?? null
        const availableEstablishmentIds = selectedEstablishmentId
          ? [selectedEstablishmentId]
          : authStoreMock.establishments.map((establishment) => establishment.id)

        return Boolean(authStoreMock.appContext?.organizationId && availableEstablishmentIds.length > 0)
      }),
    }),
  }
})

function createDeferred<T>() {
  let resolve: (value: T) => void
  let reject: (error?: unknown) => void

  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return {
    promise,
    resolve: resolve!,
    reject: reject!,
  }
}

function mountPage() {
  return mount(IKMatDashboardPage, {
    global: {
      stubs: {
        RouterLink: {
          template: '<a><slot /></a>',
        },
        DeviationsTile: {
          template: '<div>Deviations tile</div>',
        },
        ImportantDocumentsTile: {
          template: '<div>Documents tile</div>',
        },
        TemperatureTile: {
          props: ['temperaturePageTo'],
          template:
            '<div :data-route-name="temperaturePageTo?.name ?? \'\'">Temperature tile</div>',
        },
      },
    },
  })
}

describe('IKMatDashboardPage', () => {
  afterEach(() => {
    listChecklistRunsMock.mockReset()
    authStoreMock.isSessionReady = true
    authStoreMock.isStartupPending = false
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
    }
    authStoreMock.establishments = []
    authStoreMock.requiresEstablishmentSelection = false
  })

  it('renders a loading state while checklist runs are being fetched', async () => {
    const deferred = createDeferred<{
      items: []
      page: number
      size: number
      totalElements: number
      totalPages: number
    }>()

    listChecklistRunsMock.mockReturnValue(deferred.promise)

    const wrapper = mountPage()
    await nextTick()

    expect(wrapper.text()).toContain('Loading checklist runs...')
  })

  it('renders checklist summary details after a successful request', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-1',
          checklistDefinitionId: 'definition-1',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Morning shift',
          description: 'Opening routine',
          dueAt: '2026-03-26T08:00:00Z',
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: '2026-03-26T07:00:00Z',
          updatedAt: '2026-03-26T07:00:00Z',
          assignments: [],
          tasks: [],
          events: [],
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Checklists')
    expect(wrapper.text()).toContain('Temperature tile')
    expect(wrapper.find('[data-route-name="ik-mat-temperature"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('1 active run')
    expect(wrapper.text()).toContain('0 overdue • 0 in progress')
  })

  it('counts only the latest run for the same checklist definition group', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-older',
          checklistDefinitionId: 'definition-1',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Morning shift',
          description: 'Opening routine',
          dueAt: '2026-03-26T08:00:00Z',
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: '2026-03-26T07:00:00Z',
          updatedAt: '2026-03-26T07:00:00Z',
          assignments: [],
          tasks: [],
          events: [],
        },
        {
          id: 'run-latest',
          checklistDefinitionId: 'definition-2',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Morning shift (edited)',
          description: 'Opening routine',
          dueAt: '2026-03-26T10:00:00Z',
          status: 'IN_PROGRESS',
          startedAt: '2026-03-26T09:15:00Z',
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: '2026-03-26T09:00:00Z',
          updatedAt: '2026-03-26T09:30:00Z',
          assignments: [],
          tasks: [],
          events: [],
        },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('1 active run')
    expect(wrapper.text()).toContain('0 overdue • 1 in progress')
  })

  it('keeps runs from different establishments when they share a checklist definition group', async () => {
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: null,
    }
    authStoreMock.establishments = [
      {
        id: 'est-1',
        name: 'Restaurant',
        type: 'RESTAURANT',
        status: 'ACTIVE',
        organizationId: 'org-1',
        createdAt: '2026-03-26T07:00:00Z',
        updatedAt: '2026-03-26T07:00:00Z',
      },
      {
        id: 'est-2',
        name: 'Bar',
        type: 'BAR',
        status: 'ACTIVE',
        organizationId: 'org-1',
        createdAt: '2026-03-26T07:00:00Z',
        updatedAt: '2026-03-26T07:00:00Z',
      },
    ]

    listChecklistRunsMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'run-est-1',
            checklistDefinitionId: 'definition-1',
            definitionGroupId: 'group-1',
            establishmentId: 'est-1',
            serviceArea: 'IK_MAT',
            title: 'Restaurant morning shift',
            description: 'Opening routine',
            dueAt: '2026-03-26T08:00:00Z',
            status: 'PENDING',
            startedAt: null,
            completedAt: null,
            completedByUserId: null,
            createdByUserId: 'user-1',
            createdAt: '2026-03-26T07:00:00Z',
            updatedAt: '2026-03-26T07:00:00Z',
            assignments: [],
            tasks: [],
            events: [],
          },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'run-est-2',
            checklistDefinitionId: 'definition-2',
            definitionGroupId: 'group-1',
            establishmentId: 'est-2',
            serviceArea: 'IK_MAT',
            title: 'Bar morning shift',
            description: 'Opening routine',
            dueAt: '2026-03-26T09:00:00Z',
            status: 'IN_PROGRESS',
            startedAt: '2026-03-26T08:30:00Z',
            completedAt: null,
            completedByUserId: null,
            createdByUserId: 'user-1',
            createdAt: '2026-03-26T08:00:00Z',
            updatedAt: '2026-03-26T08:30:00Z',
            assignments: [],
            tasks: [],
            events: [],
          },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('2 active runs')
    expect(wrapper.text()).toContain('0 overdue • 1 in progress')
  })

  it('renders an empty state when no checklist runs are returned', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('No checklist runs found.')
  })

  it('renders an api error message when the request fails', async () => {
    listChecklistRunsMock.mockRejectedValue(new ApiError('Forbidden', 403))

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Forbidden')
  })

  it('renders a generic missing context message outside development', async () => {
    authStoreMock.appContext = null
    authStoreMock.establishments = []

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain(
      'Checklist runs cannot be loaded until organization and establishment context is available.',
    )
    expect(listChecklistRunsMock).not.toHaveBeenCalled()
  })
})
