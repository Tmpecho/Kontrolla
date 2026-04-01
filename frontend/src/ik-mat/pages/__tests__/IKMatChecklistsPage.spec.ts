import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import IKMatChecklistsPage from '@/ik-mat/pages/IKMatChecklistsPage.vue'
import { ApiError } from '@/shared/api/http'

const { listChecklistRunsMock, appEnvMock } = vi.hoisted(() => ({
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
}))

vi.mock('@/checklists/api/checklist-runs.api', () => ({
  listChecklistRuns: listChecklistRunsMock,
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

function mountPage() {
  return mount(IKMatChecklistsPage, {
    global: {
      stubs: {
        ChecklistRunCard: {
          props: ['run'],
          template: '<div class="run-card-stub">{{ run.title }}</div>',
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

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('IK-mat Checklists')
    expect(wrapper.text()).toContain('Morning shift')
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
    appEnvMock.isDevelopment = false
    appEnvMock.isProduction = true
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain(
      'Checklist runs cannot be loaded until organization and establishment context is available.',
    )
    expect(listChecklistRunsMock).not.toHaveBeenCalled()
  })
})
