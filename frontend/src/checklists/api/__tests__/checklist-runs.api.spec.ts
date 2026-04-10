import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'
import { ApiError } from '@/shared/api/http'
import { listAllChecklistRuns, listChecklistRuns } from '@/checklists/api/checklist-runs.api'

vi.mock('@/auth/model/auth.store', () => ({
  getAccessToken: () => 'test-access-token',
}))

vi.mock('@/shared/api/csrf', () => ({
  getCsrfHeaders: async () => ({}),
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: {
    mode: 'test',
    isDevelopment: true,
    isProduction: false,
    apiBaseUrl: 'http://localhost:8080',
    defaultOrganizationId: undefined,
    defaultEstablishmentId: undefined,
    showDevLoginHint: false,
  },
}))

describe('checklist-runs.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('requests checklist runs for the requested service area without statuses when none are provided', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        items: [],
        page: 1,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listChecklistRuns({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      page: 1,
      size: 10,
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/checklists/runs?serviceArea=IK_MAT&page=1&size=10',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
        headers: expect.objectContaining({
          get: expect.any(Function),
        }),
      }),
    )

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer test-access-token')
  })

  it('serializes repeated statuses query parameters when status filters are provided', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        items: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listChecklistRuns({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      statuses: ['PENDING', 'OVERDUE'],
      page: 0,
      size: 10,
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/checklists/runs?serviceArea=IK_MAT&statuses=PENDING&statuses=OVERDUE&page=0&size=10',
      expect.anything(),
    )
  })

  it('lists checklist runs across every page with the same filters', async () => {
    const fetchMock = fetch as Mock
    fetchMock
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          items: [{ id: 'run-1' }],
          page: 0,
          size: 1,
          totalElements: 2,
          totalPages: 2,
        }), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          items: [{ id: 'run-2' }],
          page: 1,
          size: 1,
          totalElements: 2,
          totalPages: 2,
        }), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        }),
      )

    const runs = await listAllChecklistRuns({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      statuses: ['PENDING'],
      size: 1,
    })

    expect(runs).toEqual([{ id: 'run-1' }, { id: 'run-2' }])
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/checklists/runs?serviceArea=IK_MAT&statuses=PENDING&page=0&size=1',
      expect.anything(),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/checklists/runs?serviceArea=IK_MAT&statuses=PENDING&page=1&size=1',
      expect.anything(),
    )
  })

  it('throws a parsed api error when the request fails', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ message: 'Forbidden' }), {
        status: 403,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await expect(
      listChecklistRuns({
        organizationId: 'org-1',
        establishmentId: 'est-1',
        serviceArea: 'IK_MAT',
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Forbidden',
      status: 403,
    } satisfies Partial<ApiError>)
  })
})
