import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import WorkspaceHomePage from '@/workspace/pages/WorkspaceHomePage.vue'

const { authStoreMock, listChecklistRunsMock, listEstablishmentDeviationsMock } = vi.hoisted(() => ({
  authStoreMock: {
    isSessionReady: true,
    isAuthenticated: true,
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
  },
  listChecklistRunsMock: vi.fn(),
  listEstablishmentDeviationsMock: vi.fn(),
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/checklists/api/checklist-runs.api', () => ({
  listChecklistRuns: listChecklistRunsMock,
}))

vi.mock('@/deviations/api/deviations.api', () => ({
  listEstablishmentDeviations: listEstablishmentDeviationsMock,
}))

describe('WorkspaceHomePage', () => {
  afterEach(() => {
    listChecklistRunsMock.mockReset()
    listEstablishmentDeviationsMock.mockReset()
  })

  it('aggregates checklist and deviation summaries across establishments when none is selected', async () => {
    listChecklistRunsMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'run-est-1',
            checklistDefinitionId: 'definition-1',
            definitionGroupId: 'group-1',
            establishmentId: 'est-1',
            serviceArea: 'IK_MAT',
            title: 'Restaurant opening',
            description: null,
            dueAt: '2026-04-08T08:00:00Z',
            status: 'OVERDUE',
            startedAt: null,
            completedAt: null,
            completedByUserId: null,
            createdByUserId: 'user-1',
            createdAt: '2026-04-08T07:00:00Z',
            updatedAt: '2026-04-08T07:00:00Z',
            assignments: [],
            tasks: [],
            events: [],
          },
        ],
        page: 0,
        size: 100,
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
            title: 'Bar opening',
            description: null,
            dueAt: '2026-04-08T09:00:00Z',
            status: 'IN_PROGRESS',
            startedAt: '2026-04-08T08:30:00Z',
            completedAt: null,
            completedByUserId: null,
            createdByUserId: 'user-1',
            createdAt: '2026-04-08T08:00:00Z',
            updatedAt: '2026-04-08T08:30:00Z',
            assignments: [],
            tasks: [],
            events: [],
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      })

    listEstablishmentDeviationsMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'dev-est-1',
            organizationId: 'org-1',
            establishmentId: 'est-1',
            createdByUserId: 'user-1',
            assignedToUserId: null,
            title: 'Food issue',
            description: 'Food deviation',
            status: 'OPEN',
            severity: 'HIGH',
            category: 'TEMPERATURE',
            createdAt: '2026-04-08T07:00:00Z',
            updatedAt: '2026-04-08T07:00:00Z',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'dev-est-2',
            organizationId: 'org-1',
            establishmentId: 'est-2',
            createdByUserId: 'user-1',
            assignedToUserId: null,
            title: 'Alcohol issue',
            description: 'Alcohol deviation',
            status: 'OPEN',
            severity: 'CRITICAL',
            category: 'AGE_CONTROL',
            createdAt: '2026-04-08T08:00:00Z',
            updatedAt: '2026-04-08T08:00:00Z',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      })

    const wrapper = mount(WorkspaceHomePage, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })

    await flushPromises()

    expect(listChecklistRunsMock).toHaveBeenCalledTimes(2)
    expect(listEstablishmentDeviationsMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('2 runs')
    expect(wrapper.text()).toContain('1 item')
  })
})
