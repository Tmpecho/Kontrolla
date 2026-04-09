import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DeviationPage from '@/deviations/pages/DeviationPage.vue'

const {
  addDeviationTimelineNoteMock,
  assignDeviationMock,
  getDeviationMock,
  listEstablishmentDeviationsMock,
  listOrganizationMembersMock,
  updateDeviationDetailsMock,
  updateDeviationStatusMock,
  authStoreMock,
  appEnvMock,
  routeState,
  routerReplaceMock,
} = vi.hoisted(() => ({
  addDeviationTimelineNoteMock: vi.fn(),
  assignDeviationMock: vi.fn(),
  getDeviationMock: vi.fn(),
  listEstablishmentDeviationsMock: vi.fn(),
  listOrganizationMembersMock: vi.fn(),
  updateDeviationDetailsMock: vi.fn(),
  updateDeviationStatusMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1' as string | null,
    },
    establishments: [] as Array<{ id: string; name: string }>,
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
  routeState: {
    name: 'ik-mat-deviation',
    query: {
      deviationId: 'dev-1',
    } as Record<string, unknown>,
  },
  routerReplaceMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/deviations/api/deviations.api', async () => {
  const actual = await vi.importActual<typeof import('@/deviations/api/deviations.api')>(
    '@/deviations/api/deviations.api',
  )

  return {
    ...actual,
    addDeviationTimelineNote: addDeviationTimelineNoteMock,
    assignDeviation: assignDeviationMock,
    getDeviation: getDeviationMock,
    listEstablishmentDeviations: listEstablishmentDeviationsMock,
    listOrganizationMembers: listOrganizationMembersMock,
    updateDeviationDetails: updateDeviationDetailsMock,
    updateDeviationStatus: updateDeviationStatusMock,
  }
})

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    push: vi.fn(),
    replace: routerReplaceMock,
  }),
}))

describe('DeviationPage', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    addDeviationTimelineNoteMock.mockReset()
    assignDeviationMock.mockReset()
    getDeviationMock.mockReset()
    listEstablishmentDeviationsMock.mockReset()
    listOrganizationMembersMock.mockReset()
    updateDeviationDetailsMock.mockReset()
    updateDeviationStatusMock.mockReset()
    routerReplaceMock.mockClear()
    authStoreMock.appContext.establishmentId = 'est-1'
    authStoreMock.establishments = []
    routeState.name = 'ik-mat-deviation'
    routeState.query = { deviationId: 'dev-1' }
    window.innerWidth = 1024
  })

  it('loads selected deviation details and adds a timeline note', async () => {
    listEstablishmentDeviationsMock.mockResolvedValue({
      items: [
        {
          id: 'dev-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          createdByUserId: 'user-1',
          assignedToUserId: null,
          title: 'Walk-in fridge too warm',
          description: 'Opening check measured 10C.',
          status: 'OPEN',
          severity: 'HIGH',
          category: 'TEMPERATURE',
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    listOrganizationMembersMock.mockResolvedValue({
      items: [
        {
          id: 'member-1',
          userId: 'user-1',
          userEmail: 'reporter@example.com',
          userFirstName: 'Reporter',
          userLastName: 'User',
          role: 'ORG_EMPLOYEE',
          active: true,
          allEstablishments: false,
          establishments: [{ id: 'est-1', name: 'Restaurant' }],
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    getDeviationMock.mockResolvedValue({
      id: 'dev-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      createdByUserId: 'user-1',
      assignedToUserId: null,
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      status: 'OPEN',
      severity: 'HIGH',
      category: 'TEMPERATURE',
      createdAt: '2026-04-06T08:00:00Z',
      updatedAt: '2026-04-06T08:00:00Z',
      timeline: [
        {
          id: 'evt-1',
          eventType: 'REPORTED',
          actorUserId: 'user-1',
          authorName: 'Reporter User',
          note: 'Deviation reported.',
          occurredAt: '2026-04-06T08:00:00Z',
        },
      ],
    })
    addDeviationTimelineNoteMock.mockResolvedValue({
      id: 'dev-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      createdByUserId: 'user-1',
      assignedToUserId: null,
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      status: 'OPEN',
      severity: 'HIGH',
      category: 'TEMPERATURE',
      createdAt: '2026-04-06T08:00:00Z',
      updatedAt: '2026-04-06T08:10:00Z',
      timeline: [
        {
          id: 'evt-1',
          eventType: 'REPORTED',
          actorUserId: 'user-1',
          authorName: 'Reporter User',
          note: 'Deviation reported.',
          occurredAt: '2026-04-06T08:00:00Z',
        },
        {
          id: 'evt-2',
          eventType: 'NOTE_ADDED',
          actorUserId: 'user-1',
          authorName: 'Reporter User',
          note: 'Products moved to backup cooling.',
          occurredAt: '2026-04-06T08:10:00Z',
        },
      ],
    })

    const wrapper = mount(DeviationPage)
    await flushPromises()

    expect(listEstablishmentDeviationsMock).toHaveBeenCalled()
    expect(listOrganizationMembersMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      includeInactive: true,
      size: 200,
    })
    expect(getDeviationMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      deviationId: 'dev-1',
    })
    expect(wrapper.text()).toContain('Deviation reported.')

    await wrapper.get('#deviation-timeline-note').setValue('  Products moved to backup cooling.  ')
    const addNoteButton = wrapper.findAll('button').find((candidate) => candidate.text() === 'Add note')
    await addNoteButton?.trigger('click')
    await flushPromises()

    expect(addDeviationTimelineNoteMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      deviationId: 'dev-1',
      note: 'Products moved to backup cooling.',
    })
    expect(wrapper.text()).toContain('Products moved to backup cooling.')
  })

  it('keeps inactive member names available for selected deviation details', async () => {
    listEstablishmentDeviationsMock.mockResolvedValue({
      items: [
        {
          id: 'dev-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          createdByUserId: 'user-1',
          assignedToUserId: 'user-2',
          title: 'Walk-in fridge too warm',
          description: 'Opening check measured 10C.',
          status: 'OPEN',
          severity: 'HIGH',
          category: 'TEMPERATURE',
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    listOrganizationMembersMock.mockResolvedValue({
      items: [
        {
          id: 'member-1',
          userId: 'user-1',
          userEmail: 'reporter@example.com',
          userFirstName: 'Reporter',
          userLastName: 'User',
          role: 'ORG_EMPLOYEE',
          active: true,
          allEstablishments: false,
          establishments: [{ id: 'est-1', name: 'Restaurant' }],
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
        {
          id: 'member-2',
          userId: 'user-2',
          userEmail: 'inactive@example.com',
          userFirstName: 'Inactive',
          userLastName: 'User',
          role: 'ORG_EMPLOYEE',
          active: false,
          allEstablishments: false,
          establishments: [{ id: 'est-1', name: 'Restaurant' }],
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })
    getDeviationMock.mockResolvedValue({
      id: 'dev-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      createdByUserId: 'user-1',
      assignedToUserId: 'user-2',
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      status: 'OPEN',
      severity: 'HIGH',
      category: 'TEMPERATURE',
      createdAt: '2026-04-06T08:00:00Z',
      updatedAt: '2026-04-06T08:00:00Z',
      timeline: [
        {
          id: 'evt-1',
          eventType: 'REPORTED',
          actorUserId: 'user-1',
          authorName: 'Reporter User',
          note: 'Deviation reported.',
          occurredAt: '2026-04-06T08:00:00Z',
        },
      ],
    })

    const wrapper = mount(DeviationPage)
    await flushPromises()

    expect(wrapper.text()).toContain('Inactive User')
    expect(wrapper.text()).not.toContain('User user-2')
    const updateButton = wrapper.findAll('button').find((candidate) => candidate.text() === 'Update')
    await updateButton?.trigger('click')
    await flushPromises()
    const assigneeOptions = wrapper.findAll('#deviation-assignee option')
    expect(assigneeOptions.some((option) => option.text() === 'Inactive User')).toBe(true)
  })

  it('limits assignee options to the selected deviation establishment in aggregated views', async () => {
    authStoreMock.appContext.establishmentId = null
    authStoreMock.establishments = [
      { id: 'est-1', name: 'Restaurant One' },
      { id: 'est-2', name: 'Restaurant Two' },
    ]

    listEstablishmentDeviationsMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'dev-1',
            organizationId: 'org-1',
            establishmentId: 'est-1',
            createdByUserId: 'user-1',
            assignedToUserId: null,
            title: 'Walk-in fridge too warm',
            description: 'Opening check measured 10C.',
            status: 'OPEN',
            severity: 'HIGH',
            category: 'TEMPERATURE',
            createdAt: '2026-04-06T08:00:00Z',
            updatedAt: '2026-04-06T08:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'dev-2',
            organizationId: 'org-1',
            establishmentId: 'est-2',
            createdByUserId: 'user-2',
            assignedToUserId: null,
            title: 'Missing cleaning log',
            description: 'Evening checklist incomplete.',
            status: 'OPEN',
            severity: 'MEDIUM',
            category: 'HYGIENE',
            createdAt: '2026-04-06T09:00:00Z',
            updatedAt: '2026-04-06T09:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })

    listOrganizationMembersMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'member-1',
            userId: 'user-1',
            userEmail: 'est1@example.com',
            userFirstName: 'Est',
            userLastName: 'One',
            role: 'ORG_EMPLOYEE',
            active: true,
            allEstablishments: false,
            establishments: [{ id: 'est-1', name: 'Restaurant One' }],
            createdAt: '2026-04-06T08:00:00Z',
            updatedAt: '2026-04-06T08:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'member-2',
            userId: 'user-2',
            userEmail: 'est2@example.com',
            userFirstName: 'Est',
            userLastName: 'Two',
            role: 'ORG_EMPLOYEE',
            active: true,
            allEstablishments: false,
            establishments: [{ id: 'est-2', name: 'Restaurant Two' }],
            createdAt: '2026-04-06T08:00:00Z',
            updatedAt: '2026-04-06T08:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })

    getDeviationMock.mockResolvedValue({
      id: 'dev-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      createdByUserId: 'user-1',
      assignedToUserId: null,
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      status: 'OPEN',
      severity: 'HIGH',
      category: 'TEMPERATURE',
      createdAt: '2026-04-06T08:00:00Z',
      updatedAt: '2026-04-06T08:00:00Z',
      timeline: [
        {
          id: 'evt-1',
          eventType: 'REPORTED',
          actorUserId: 'user-1',
          authorName: 'Est One',
          note: 'Deviation reported.',
          occurredAt: '2026-04-06T08:00:00Z',
        },
      ],
    })

    const wrapper = mount(DeviationPage)
    await flushPromises()

    const updateButton = wrapper.findAll('button').find((candidate) => candidate.text() === 'Update')
    await updateButton?.trigger('click')
    await flushPromises()

    const assigneeOptions = wrapper.findAll('#deviation-assignee option').map((option) => option.text())
    expect(assigneeOptions).toContain('Unassigned')
    expect(assigneeOptions).toContain('Est One')
    expect(assigneeOptions).not.toContain('Est Two')
  })

  it('renders selected deviation details in the shared mobile drawer and closes on backdrop click', async () => {
    window.innerWidth = 900

    listEstablishmentDeviationsMock.mockResolvedValue({
      items: [
        {
          id: 'dev-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          createdByUserId: 'user-1',
          assignedToUserId: null,
          title: 'Walk-in fridge too warm',
          description: 'Opening check measured 10C.',
          status: 'OPEN',
          severity: 'HIGH',
          category: 'TEMPERATURE',
          createdAt: '2026-04-06T08:00:00Z',
          updatedAt: '2026-04-06T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    listOrganizationMembersMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 1,
    })
    getDeviationMock.mockResolvedValue({
      id: 'dev-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      createdByUserId: 'user-1',
      assignedToUserId: null,
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      status: 'OPEN',
      severity: 'HIGH',
      category: 'TEMPERATURE',
      createdAt: '2026-04-06T08:00:00Z',
      updatedAt: '2026-04-06T08:00:00Z',
      timeline: [],
    })

    mount(DeviationPage, { attachTo: document.body })
    await flushPromises()

    expect(document.body.querySelector('.app-overlay-panel')).not.toBeNull()
    expect(document.body.textContent).toContain('Walk-in fridge too warm')

    const backdrop = document.body.querySelector('.app-overlay-backdrop')
    expect(backdrop).not.toBeNull()

    backdrop?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()

    expect(routerReplaceMock).toHaveBeenCalledWith({
      query: {},
    })
  })
})
