import { flushPromises, mount } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import WorkspaceHomePage from '@/workspace/pages/WorkspaceHomePage.vue'

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (error?: unknown) => void

  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

function createChecklistPage({
  id,
  establishmentId,
  status,
  dueAt,
}: {
  id: string
  establishmentId: string
  status: 'OVERDUE' | 'IN_PROGRESS' | 'COMPLETED'
  dueAt: string
}) {
  return {
    items: [
      {
        id,
        checklistDefinitionId: `definition-${id}`,
        definitionGroupId: `group-${id}`,
        establishmentId,
        serviceArea: 'IK_MAT' as const,
        title: `${id} checklist`,
        description: null,
        dueAt,
        status,
        startedAt: status === 'IN_PROGRESS' ? dueAt : null,
        completedAt: status === 'COMPLETED' ? dueAt : null,
        completedByUserId: null,
        createdByUserId: 'user-1',
        createdAt: dueAt,
        updatedAt: dueAt,
        assignments: [],
        tasks: [],
        events: [],
      },
    ],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  }
}

function createDeviationPage({
  id,
  establishmentId,
  category,
  severity,
}: {
  id: string
  establishmentId: string
  category: 'TEMPERATURE' | 'AGE_CONTROL'
  severity: 'HIGH' | 'CRITICAL'
}) {
  return {
    items: [
      {
        id,
        organizationId: 'org-1',
        establishmentId,
        createdByUserId: 'user-1',
        assignedToUserId: null,
        title: `${id} deviation`,
        description: `${id} description`,
        status: 'OPEN' as const,
        severity,
        category,
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
    ],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  }
}

function createDocument({
  id,
  establishmentId,
  title,
  renewalDate,
  status,
  auditAssignments = [],
}: {
  id: string
  establishmentId: string
  title: string
  renewalDate: string
  status: 'VALID' | 'EXPIRING' | 'EXPIRED'
  auditAssignments?: Array<{
    userId: string
    userEmail: string
    userFirstName: string
    userLastName: string
    acknowledgedAt: string | null
  }>
}) {
  return {
    id,
    organizationId: 'org-1',
    establishmentId,
    createdByUserId: 'user-1',
    serviceArea: 'IK_ALKOHOL' as const,
    title,
    holderName: 'Holder',
    issueDate: '2026-01-01',
    renewalDate,
    fileName: `${id}.pdf`,
    contentType: 'application/pdf',
    fileSizeBytes: 1024,
    status,
    auditAssignments,
    createdAt: '2026-01-01T08:00:00Z',
    updatedAt: '2026-01-01T08:00:00Z',
  }
}

function createTemperatureUnit({
  id,
  name,
  measuredAt,
  temperatureCelsius,
}: {
  id: string
  name: string
  measuredAt: string
  temperatureCelsius: number
}) {
  return {
    id,
    name,
    location: 'Kitchen',
    type: 'FRIDGE' as const,
    dueByTime: '08:30',
    minimumTemperature: 2,
    maximumTemperature: 4,
    logs: [
      {
        id: `${id}-log-1`,
        measuredAt,
        temperatureCelsius,
        note: null,
        loggedByName: 'Maria Nilsen',
      },
    ],
  }
}

const mocks = vi.hoisted(() => ({
  authStoreMock: null as null | {
    isSessionReady: boolean
    isStartupPending: boolean
    user: null | { id: string }
    appContext: null | {
      organizationId: string | null
      organizationName: string | null
      organizationRole: string | null
      establishmentId: string | null
      establishmentName: string | null
    }
    establishments: Array<{
      id: string
      organizationId: string
      name: string
      type: 'RESTAURANT' | 'BAR'
      status: 'ACTIVE'
      createdAt: string
      updatedAt: string
    }>
  },
  listChecklistRunsMock: vi.fn(),
  listEstablishmentDeviationsMock: vi.fn(),
  listAllEstablishmentDocumentsMock: vi.fn(),
  listTemperatureUnitsMock: vi.fn(),
}))

vi.mock('@/auth/model/auth.store', async () => {
  const { reactive } = await import('vue')

  if (!mocks.authStoreMock) {
    mocks.authStoreMock = reactive({
      isSessionReady: true,
      isStartupPending: false,
      user: {
        id: 'user-99',
      },
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Org 1',
        organizationRole: 'ORG_MANAGER',
        establishmentId: null,
        establishmentName: null,
      },
      establishments: [
        {
          id: 'est-1',
          organizationId: 'org-1',
          name: 'Restaurant',
          type: 'RESTAURANT' as const,
          status: 'ACTIVE' as const,
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
        {
          id: 'est-2',
          organizationId: 'org-1',
          name: 'Bar',
          type: 'BAR' as const,
          status: 'ACTIVE' as const,
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
    })
  }

  return {
    useAuthStore: () => mocks.authStoreMock,
  }
})

vi.mock('@/auth/model/workspace-context', async () => {
  const { computed } = await import('vue')

  return {
    useProtectedWorkspaceContext: () => ({
      organizationId: computed(() => mocks.authStoreMock?.appContext?.organizationId ?? null),
      establishmentId: computed(() => mocks.authStoreMock?.appContext?.establishmentId ?? null),
      availableEstablishmentIds: computed(() => {
        const selectedEstablishmentId = mocks.authStoreMock?.appContext?.establishmentId ?? null

        if (selectedEstablishmentId) {
          return [selectedEstablishmentId]
        }

        return mocks.authStoreMock?.establishments.map((establishment) => establishment.id) ?? []
      }),
      isStartupPending: computed(() => mocks.authStoreMock?.isStartupPending ?? false),
      requiresEstablishmentSelection: computed(() => false),
      hasOrganizationContext: computed(() => Boolean(mocks.authStoreMock?.appContext?.organizationId)),
      hasEstablishmentContext: computed(() => {
        return Boolean(
          mocks.authStoreMock?.appContext?.organizationId &&
            mocks.authStoreMock?.appContext?.establishmentId,
        )
      }),
      hasAccessibleEstablishmentContext: computed(() => {
        const selectedEstablishmentId = mocks.authStoreMock?.appContext?.establishmentId ?? null
        const availableEstablishmentIds = selectedEstablishmentId
          ? [selectedEstablishmentId]
          : mocks.authStoreMock?.establishments.map((establishment) => establishment.id) ?? []

        return Boolean(
          mocks.authStoreMock?.appContext?.organizationId && availableEstablishmentIds.length > 0
        )
      }),
    }),
  }
})

vi.mock('@/checklists/api/checklist-runs.api', () => ({
  listChecklistRuns: mocks.listChecklistRunsMock,
}))

vi.mock('@/deviations/api/deviations.api', () => ({
  listEstablishmentDeviations: mocks.listEstablishmentDeviationsMock,
}))

vi.mock('@/documents/api/documents.api', () => ({
  listAllEstablishmentDocuments: mocks.listAllEstablishmentDocumentsMock,
}))

vi.mock('@/ik-mat/api/temperature.api', () => ({
  listTemperatureUnits: mocks.listTemperatureUnitsMock,
}))

function resetAuthStore() {
  if (!mocks.authStoreMock) {
    throw new Error('authStoreMock was not initialized')
  }

  mocks.authStoreMock.isSessionReady = true
  mocks.authStoreMock.isStartupPending = false
  mocks.authStoreMock.user = {
    id: 'user-99',
  }
  mocks.authStoreMock.appContext = {
    organizationId: 'org-1',
    organizationName: 'Org 1',
    organizationRole: 'ORG_MANAGER',
    establishmentId: null,
    establishmentName: null,
  }
  mocks.authStoreMock.establishments = [
    {
      id: 'est-1',
      organizationId: 'org-1',
      name: 'Restaurant',
      type: 'RESTAURANT',
      status: 'ACTIVE',
      createdAt: '2026-04-08T08:00:00Z',
      updatedAt: '2026-04-08T08:00:00Z',
    },
    {
      id: 'est-2',
      organizationId: 'org-1',
      name: 'Bar',
      type: 'BAR',
      status: 'ACTIVE',
      createdAt: '2026-04-08T08:00:00Z',
      updatedAt: '2026-04-08T08:00:00Z',
    },
  ]
}

let currentWrapper: VueWrapper | null = null

function mountPage() {
  currentWrapper = mount(WorkspaceHomePage, {
    global: {
      stubs: {
        RouterLink: {
          template: '<a><slot /></a>',
        },
      },
    },
  })

  return currentWrapper
}

describe('WorkspaceHomePage', () => {
  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
    mocks.listChecklistRunsMock.mockReset()
    mocks.listEstablishmentDeviationsMock.mockReset()
    mocks.listAllEstablishmentDocumentsMock.mockReset()
    mocks.listTemperatureUnitsMock.mockReset()
    resetAuthStore()
  })

  it('aggregates live workspace data across establishments when none is selected', async () => {
    mocks.listChecklistRunsMock
      .mockResolvedValueOnce(
        createChecklistPage({
          id: 'run-est-1',
          establishmentId: 'est-1',
          status: 'OVERDUE',
          dueAt: '2026-04-08T08:00:00Z',
        }),
      )
      .mockResolvedValueOnce(
        createChecklistPage({
          id: 'run-est-2',
          establishmentId: 'est-2',
          status: 'IN_PROGRESS',
          dueAt: '2026-04-08T09:00:00Z',
        }),
      )

    mocks.listEstablishmentDeviationsMock
      .mockResolvedValueOnce(
        createDeviationPage({
          id: 'dev-est-1',
          establishmentId: 'est-1',
          category: 'TEMPERATURE',
          severity: 'HIGH',
        }),
      )
      .mockResolvedValueOnce(
        createDeviationPage({
          id: 'dev-est-2',
          establishmentId: 'est-2',
          category: 'AGE_CONTROL',
          severity: 'CRITICAL',
        }),
      )

    mocks.listAllEstablishmentDocumentsMock
      .mockResolvedValueOnce([
        createDocument({
          id: 'doc-est-1',
          establishmentId: 'est-1',
          title: 'Alcohol licence',
          renewalDate: '2026-04-12',
          status: 'EXPIRING',
          auditAssignments: [
            {
              userId: 'user-99',
              userEmail: 'auditor@example.com',
              userFirstName: 'Audit',
              userLastName: 'User',
              acknowledgedAt: null,
            },
          ],
        }),
      ])
      .mockResolvedValueOnce([
        createDocument({
          id: 'doc-est-2',
          establishmentId: 'est-2',
          title: 'Security agreement',
          renewalDate: '2026-04-01',
          status: 'EXPIRED',
        }),
      ])

    mocks.listTemperatureUnitsMock
      .mockResolvedValueOnce([
        createTemperatureUnit({
          id: 'temp-est-1',
          name: 'Walk-in cooler',
          measuredAt: '2026-04-10T07:50:00Z',
          temperatureCelsius: 5.8,
        }),
      ])
      .mockResolvedValueOnce([
        createTemperatureUnit({
          id: 'temp-est-2',
          name: 'Dessert freezer',
          measuredAt: '2026-04-10T08:05:00Z',
          temperatureCelsius: 3.5,
        }),
      ])

    const wrapper = mountPage()
    await flushPromises()

    expect(mocks.listChecklistRunsMock).toHaveBeenCalledTimes(2)
    expect(mocks.listEstablishmentDeviationsMock).toHaveBeenCalledTimes(2)
    expect(mocks.listAllEstablishmentDocumentsMock).toHaveBeenCalledTimes(2)
    expect(mocks.listTemperatureUnitsMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('2 runs')
    expect(wrapper.text()).toContain('1 unit')
    expect(wrapper.text()).toContain('2 documents')
    expect(wrapper.text()).toContain('1 document')
    expect(wrapper.text()).toContain('Security agreement')
    expect(wrapper.text()).toContain('Walk-in cooler')
  })

  it('uses only the selected establishment when workspace context is narrowed', async () => {
    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: 'org-1',
      organizationName: 'Org 1',
      organizationRole: 'ORG_MANAGER',
      establishmentId: 'est-2',
      establishmentName: 'Bar',
    }

    mocks.listChecklistRunsMock.mockResolvedValue(
      createChecklistPage({
        id: 'run-est-2',
        establishmentId: 'est-2',
        status: 'IN_PROGRESS',
        dueAt: '2026-04-08T09:00:00Z',
      }),
    )
    mocks.listEstablishmentDeviationsMock.mockResolvedValue(
      createDeviationPage({
        id: 'dev-est-2',
        establishmentId: 'est-2',
        category: 'AGE_CONTROL',
        severity: 'CRITICAL',
      }),
    )
    mocks.listAllEstablishmentDocumentsMock.mockResolvedValue([
      createDocument({
        id: 'doc-est-2',
        establishmentId: 'est-2',
        title: 'Selected establishment document',
        renewalDate: '2026-06-20',
        status: 'VALID',
      }),
    ])
    mocks.listTemperatureUnitsMock.mockResolvedValue([
      createTemperatureUnit({
        id: 'temp-est-2',
        name: 'Selected establishment fridge',
        measuredAt: '2026-04-10T08:05:00Z',
        temperatureCelsius: 3.5,
      }),
    ])

    mountPage()
    await flushPromises()

    expect(mocks.listChecklistRunsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-2',
      serviceArea: 'IK_MAT',
      page: 0,
      size: 100,
    })
    expect(mocks.listEstablishmentDeviationsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-2',
      page: 0,
      size: 100,
    })
    expect(mocks.listAllEstablishmentDocumentsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-2',
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })
    expect(mocks.listTemperatureUnitsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-2',
    })
  })

  it('shows unavailable placeholders when documents cannot be loaded', async () => {
    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: 'org-1',
      organizationName: 'Org 1',
      organizationRole: 'ORG_MANAGER',
      establishmentId: 'est-1',
      establishmentName: 'Restaurant',
    }

    mocks.listChecklistRunsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    mocks.listEstablishmentDeviationsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    mocks.listAllEstablishmentDocumentsMock.mockRejectedValue(new Error('boom'))
    mocks.listTemperatureUnitsMock.mockResolvedValue([])

    const wrapper = mountPage()
    await flushPromises()

    const alcoholMetrics = wrapper
      .find('[data-service="ik-alkohol"]')
      .findAll('.service-metric-value')
      .map((node) => node.text())

    expect(alcoholMetrics).toEqual(['0 items', '—', '—'])
    expect(wrapper.text()).toContain('Documents overview is temporarily unavailable.')
    expect(wrapper.text()).toContain(
      'Workspace attention is partially unavailable while some overview data could not be loaded.',
    )
    expect(wrapper.text()).not.toContain('No urgent follow-up right now.')
  })

  it('shows unavailable placeholders when temperature units cannot be loaded', async () => {
    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: 'org-1',
      organizationName: 'Org 1',
      organizationRole: 'ORG_MANAGER',
      establishmentId: 'est-1',
      establishmentName: 'Restaurant',
    }

    mocks.listChecklistRunsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    mocks.listEstablishmentDeviationsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    mocks.listAllEstablishmentDocumentsMock.mockResolvedValue([])
    mocks.listTemperatureUnitsMock.mockRejectedValue(new Error('boom'))

    const wrapper = mountPage()
    await flushPromises()

    const ikMatMetrics = wrapper
      .find('[data-service="ik-mat"]')
      .findAll('.service-metric-value')
      .map((node) => node.text())

    expect(ikMatMetrics).toEqual(['0 runs', '—', '0 items'])
    expect(wrapper.text()).toContain('Temperature overview is temporarily unavailable.')
    expect(wrapper.text()).toContain(
      'Workspace attention is partially unavailable while some overview data could not be loaded.',
    )
    expect(wrapper.text()).not.toContain('No urgent follow-up right now.')
  })

  it('ignores stale overview responses after the workspace context changes', async () => {
    const firstChecklistRequest = createDeferred<ReturnType<typeof createChecklistPage>>()
    const firstDeviationRequest = createDeferred<ReturnType<typeof createDeviationPage>>()
    const firstDocumentRequest = createDeferred<ReturnType<typeof createDocument>[]>()
    const firstTemperatureRequest = createDeferred<ReturnType<typeof createTemperatureUnit>[]>()

    mocks.listChecklistRunsMock.mockImplementation(({ organizationId }: { organizationId: string }) => {
      if (organizationId === 'org-1') {
        return firstChecklistRequest.promise
      }

      return Promise.resolve(createChecklistPage({
        id: 'current-run',
        establishmentId: 'est-2',
        status: 'IN_PROGRESS',
        dueAt: '2026-04-10T09:00:00Z',
      }))
    })

    mocks.listEstablishmentDeviationsMock.mockImplementation(({ organizationId }: { organizationId: string }) => {
      if (organizationId === 'org-1') {
        return firstDeviationRequest.promise
      }

      return Promise.resolve(createDeviationPage({
        id: 'current-dev',
        establishmentId: 'est-2',
        category: 'TEMPERATURE',
        severity: 'HIGH',
      }))
    })

    mocks.listAllEstablishmentDocumentsMock.mockImplementation(
      ({ organizationId }: { organizationId: string }) => {
        if (organizationId === 'org-1') {
          return firstDocumentRequest.promise
        }

        return Promise.resolve([
          createDocument({
            id: 'current-doc',
            establishmentId: 'est-2',
            title: 'Current document',
            renewalDate: '2026-04-12',
            status: 'EXPIRING',
          }),
        ])
      },
    )

    mocks.listTemperatureUnitsMock.mockImplementation(({ organizationId }: { organizationId: string }) => {
      if (organizationId === 'org-1') {
        return firstTemperatureRequest.promise
      }

      return Promise.resolve([
        createTemperatureUnit({
          id: 'current-temp',
          name: 'Current fridge',
          measuredAt: '2026-04-10T08:05:00Z',
          temperatureCelsius: 5.2,
        }),
      ])
    })

    const wrapper = mountPage()
    await nextTick()

    if (!mocks.authStoreMock) {
      throw new Error('authStoreMock was not initialized')
    }

    mocks.authStoreMock.appContext = {
      organizationId: 'org-2',
      organizationName: 'Org 2',
      organizationRole: 'ORG_MANAGER',
      establishmentId: 'est-2',
      establishmentName: 'Bar',
    }
    mocks.authStoreMock.establishments = [
      {
        id: 'est-2',
        organizationId: 'org-2',
        name: 'Bar',
        type: 'BAR',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
    ]

    await nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain('Current document')
    expect(wrapper.text()).toContain('Current fridge')

    firstChecklistRequest.resolve(createChecklistPage({
      id: 'stale-run',
      establishmentId: 'est-1',
      status: 'OVERDUE',
      dueAt: '2026-04-08T08:00:00Z',
    }))
    firstDeviationRequest.resolve(createDeviationPage({
      id: 'stale-dev',
      establishmentId: 'est-1',
      category: 'AGE_CONTROL',
      severity: 'CRITICAL',
    }))
    firstDocumentRequest.resolve([
      createDocument({
        id: 'stale-doc',
        establishmentId: 'est-1',
        title: 'Stale document',
        renewalDate: '2026-04-12',
        status: 'EXPIRING',
      }),
    ])
    firstTemperatureRequest.resolve([
      createTemperatureUnit({
        id: 'stale-temp',
        name: 'Stale fridge',
        measuredAt: '2026-04-10T07:50:00Z',
        temperatureCelsius: 5.8,
      }),
    ])
    await flushPromises()

    expect(wrapper.text()).toContain('Current document')
    expect(wrapper.text()).toContain('Current fridge')
    expect(wrapper.text()).not.toContain('Stale document')
    expect(wrapper.text()).not.toContain('Stale fridge')
  })
})
