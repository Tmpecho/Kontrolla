import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import IKMatChecklistsPage from '@/ik-mat/pages/IKMatChecklistsPage.vue'
import { ApiError } from '@/shared/api/http'

const { listChecklistRunsMock, appEnvMock, authStoreMock } = vi.hoisted(() => ({
  listChecklistRunsMock: vi.fn(),
  appEnvMock: {
    mode: 'test',
    isDevelopment: true,
    isProduction: false,
    apiBaseUrl: 'http://localhost:8080',
    defaultOrganizationId: 'org-1' as string | undefined,
    defaultEstablishmentId: 'est-1' as string | undefined,
    showDevLoginHint: false,
  },
  authStoreMock: {
    isSessionReady: true,
    isAuthenticated: false,
    appContext: null as
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

vi.mock('@/checklists/components/ChecklistDefinitionManager.vue', () => ({
  default: {
    name: 'ChecklistDefinitionManager',
    template: '<div class="checklist-definition-manager-stub"></div>',
  },
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

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

async function mountPage(query: Record<string, string> = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/app/ik-mat/checklists', name: 'ik-mat-checklists', component: IKMatChecklistsPage }],
  })

  await router.push({name: 'ik-mat-checklists', query})
  await router.isReady()

  return mount(IKMatChecklistsPage, {
    global: {
      plugins: [router],
      stubs: {
        ChecklistRunCard: {
          props: ['run', 'selected', 'forceExpanded'],
          template:
            '<div class="run-card-stub" :data-selected="selected" :data-force-expanded="forceExpanded">{{ run.title }}</div>',
        },
      },
    },
  })
}

describe('IKMatChecklistsPage', () => {
  afterEach(() => {
    listChecklistRunsMock.mockReset()
    appEnvMock.isDevelopment = true
    appEnvMock.isProduction = false
    appEnvMock.defaultOrganizationId = 'org-1'
    appEnvMock.defaultEstablishmentId = 'est-1'
    authStoreMock.isSessionReady = true
    authStoreMock.isAuthenticated = false
    authStoreMock.appContext = null
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

    const wrapper = await mountPage()
    await nextTick()

    expect(wrapper.text()).toContain('Loading checklist runs...')
  })

  it('renders checklist runs after a successful request', async () => {
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

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('IK-mat Checklists')
    expect(wrapper.text()).toContain('Morning shift')
  })

  it('renders all upcoming runs even when they share a checklist definition group', async () => {
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
          status: 'PENDING',
          startedAt: null,
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

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Morning shift')
    expect(wrapper.text()).toContain('Morning shift (edited)')
    expect(wrapper.findAll('.run-card-stub')).toHaveLength(2)
  })

  it('renders all pending runs in the upcoming triage tab', async () => {
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
          status: 'PENDING',
          startedAt: null,
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

    const wrapper = await mountPage()
    await flushPromises()

    const triageTabs = wrapper.findAll('.triage-tab')
    await triageTabs[0]!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('Morning shift')
    expect(wrapper.text()).toContain('Morning shift (edited)')
    expect(wrapper.findAll('.run-card-stub')).toHaveLength(2)
    expect(wrapper.findAll('.date-group')).toHaveLength(1)
    expect(wrapper.find('.date-group').text()).toContain('2 runs')
  })

  it('groups visible checklist runs by due date', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-day-1-a',
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
          id: 'run-day-1-b',
          checklistDefinitionId: 'definition-2',
          definitionGroupId: 'group-2',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Midday shift',
          description: 'Prep routine',
          dueAt: '2026-03-26T10:00:00Z',
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: '2026-03-26T09:00:00Z',
          updatedAt: '2026-03-26T09:30:00Z',
          assignments: [],
          tasks: [],
          events: [],
        },
        {
          id: 'run-day-2',
          checklistDefinitionId: 'definition-3',
          definitionGroupId: 'group-3',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Closing shift',
          description: 'Closing routine',
          dueAt: '2026-03-27T18:00:00Z',
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: '2026-03-27T17:00:00Z',
          updatedAt: '2026-03-27T17:00:00Z',
          assignments: [],
          tasks: [],
          events: [],
        },
      ],
      page: 0,
      size: 10,
      totalElements: 3,
      totalPages: 1,
    })

    const wrapper = await mountPage()
    await flushPromises()

    const triageTabs = wrapper.findAll('.triage-tab')
    await triageTabs[0]!.trigger('click')
    await nextTick()

    const groups = wrapper.findAll('.date-group')
    expect(groups).toHaveLength(2)
    expect(groups[0]!.text()).toContain('2 runs')
    expect(groups[1]!.text()).toContain('1 run')
  })

  it('shows due-today runs even when a newer run exists in the same definition group', async () => {
    const now = new Date()
    const todayAt = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 10, 0, 0)
    const tomorrowAt = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 10, 0, 0)

    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-today',
          checklistDefinitionId: 'definition-1',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Today run',
          description: 'Should be visible in due today',
          dueAt: todayAt.toISOString(),
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: todayAt.toISOString(),
          updatedAt: todayAt.toISOString(),
          assignments: [],
          tasks: [],
          events: [],
        },
        {
          id: 'run-tomorrow',
          checklistDefinitionId: 'definition-2',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Tomorrow run',
          description: 'Newer instance in same group',
          dueAt: tomorrowAt.toISOString(),
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: tomorrowAt.toISOString(),
          updatedAt: tomorrowAt.toISOString(),
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

    const wrapper = await mountPage()
    await flushPromises()

    const triageTabs = wrapper.findAll('.triage-tab')
    await triageTabs[2]!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('Today run')
    expect(wrapper.text()).not.toContain('Tomorrow run')
    expect(wrapper.findAll('.run-card-stub')).toHaveLength(1)
  })

  it('keeps due-today runs out of the upcoming tab', async () => {
    const now = new Date()
    const todayAt = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 10, 0, 0)
    const tomorrowAt = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 10, 0, 0)

    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-today',
          checklistDefinitionId: 'definition-1',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Today run',
          description: 'Should stay out of upcoming',
          dueAt: todayAt.toISOString(),
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: todayAt.toISOString(),
          updatedAt: todayAt.toISOString(),
          assignments: [],
          tasks: [],
          events: [],
        },
        {
          id: 'run-tomorrow',
          checklistDefinitionId: 'definition-2',
          definitionGroupId: 'group-2',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Tomorrow run',
          description: 'Should stay in upcoming',
          dueAt: tomorrowAt.toISOString(),
          status: 'PENDING',
          startedAt: null,
          completedAt: null,
          completedByUserId: null,
          createdByUserId: 'user-1',
          createdAt: tomorrowAt.toISOString(),
          updatedAt: tomorrowAt.toISOString(),
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

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Tomorrow run')
    expect(wrapper.text()).not.toContain('Today run')
    expect(wrapper.findAll('.run-card-stub')).toHaveLength(1)
  })

  it('keeps runs from different establishments when they share a checklist definition group', async () => {
    authStoreMock.isAuthenticated = true
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
            status: 'PENDING',
            startedAt: null,
            completedAt: null,
            completedByUserId: null,
            createdByUserId: 'user-1',
            createdAt: '2026-03-26T08:00:00Z',
            updatedAt: '2026-03-26T08:00:00Z',
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

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Restaurant morning shift')
    expect(wrapper.text()).toContain('Bar morning shift')
    expect(wrapper.findAll('.run-card-stub')).toHaveLength(2)
  })

  it('renders an empty state when no checklist runs are returned', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('No checklist runs found.')
  })

  it('renders an api error message when the request fails', async () => {
    listChecklistRunsMock.mockRejectedValue(new ApiError('Forbidden', 403))

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Forbidden')
  })

  it('keeps the queried checklist run visible and selected outside the active triage filter', async () => {
    listChecklistRunsMock.mockResolvedValue({
      items: [
        {
          id: 'run-overdue',
          checklistDefinitionId: 'definition-1',
          definitionGroupId: 'group-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Overdue checklist',
          description: 'Needs attention now',
          dueAt: '2026-03-26T08:00:00Z',
          status: 'OVERDUE',
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
          id: 'run-completed',
          checklistDefinitionId: 'definition-2',
          definitionGroupId: 'group-2',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          title: 'Completed checklist',
          description: 'Already done',
          dueAt: '2026-03-26T10:00:00Z',
          status: 'COMPLETED',
          startedAt: '2026-03-26T09:45:00Z',
          completedAt: '2026-03-26T10:30:00Z',
          completedByUserId: 'user-1',
          createdByUserId: 'user-1',
          createdAt: '2026-03-26T09:00:00Z',
          updatedAt: '2026-03-26T10:30:00Z',
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

    const wrapper = await mountPage({ checklistRunId: 'run-completed' })
    await flushPromises()

    const runCards = wrapper.findAll('.run-card-stub')
    const selectedRunCard = runCards.find((card) => card.attributes('data-selected') === 'true')

    expect(runCards).toHaveLength(1)
    expect(selectedRunCard?.text()).toContain('Completed checklist')
    expect(selectedRunCard?.attributes('data-selected')).toBe('true')
    expect(selectedRunCard?.attributes('data-force-expanded')).toBe('true')
  })

  it('keeps a run pinned until the triage tab changes after its status changes', async () => {
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

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/app/ik-mat/checklists', name: 'ik-mat-checklists', component: IKMatChecklistsPage }],
    })
    await router.push({name: 'ik-mat-checklists'})
    await router.isReady()

    const wrapper = mount(IKMatChecklistsPage, {
      global: {
        plugins: [router],
        stubs: {
          ChecklistRunCard: {
            props: ['run', 'selected', 'forceExpanded'],
            emits: ['update:run'],
            template:
              '<button class="run-card-stub" :data-selected="selected" :data-force-expanded="forceExpanded" @click="$emit(\'update:run\', { ...run, status: \'COMPLETED\' })">{{ run.title }}</button>',
          },
        },
      },
    })

    await flushPromises()

    expect(wrapper.findAll('.run-card-stub')).toHaveLength(1)

    await wrapper.get('.run-card-stub').trigger('click')
    await nextTick()

    expect(wrapper.findAll('.run-card-stub')).toHaveLength(1)
    expect(wrapper.get('.run-card-stub').attributes('data-selected')).toBe('true')
    expect(wrapper.get('.run-card-stub').attributes('data-force-expanded')).toBe('true')
    expect(wrapper.text()).toContain('Morning shift')

    const triageTabs = wrapper.findAll('.triage-tab')

    await triageTabs[4]!.trigger('click')
    await nextTick()

    expect(wrapper.findAll('.run-card-stub')).toHaveLength(1)
    expect(wrapper.text()).toContain('Morning shift')

    await triageTabs[0]!.trigger('click')
    await nextTick()

    expect(wrapper.findAll('.run-card-stub')).toHaveLength(0)
    expect(wrapper.text()).toContain('No checklist runs match the current filter.')
  })

  it('renders a generic missing context message outside development', async () => {
    appEnvMock.isDevelopment = false
    appEnvMock.isProduction = true
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain(
      'Checklist runs cannot be loaded until organization and establishment context is available.',
    )
    expect(listChecklistRunsMock).not.toHaveBeenCalled()
  })
})
