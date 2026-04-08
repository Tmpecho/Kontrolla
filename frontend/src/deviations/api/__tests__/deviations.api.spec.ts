import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'
import {
  addDeviationTimelineNote,
  getDeviation,
  mapDeviationResponseToListItem,
} from '@/deviations/api/deviations.api'

vi.mock('@/auth/model/auth.store', () => ({
  getAccessToken: () => 'test-access-token',
}))

vi.mock('@/shared/api/csrf', () => ({
  getCsrfHeaders: async (method: string) => (method === 'GET' ? {} : { 'X-XSRF-TOKEN': 'csrf-token' }),
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

describe('deviations.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('requests deviation details from the detail endpoint', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
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
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await getDeviation({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      deviationId: 'dev-1',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/deviations/dev-1',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })

  it('posts timeline notes to the timeline endpoint', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
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
        updatedAt: '2026-04-06T08:05:00Z',
        timeline: [],
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await addDeviationTimelineNote({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      deviationId: 'dev-1',
      note: 'Follow-up completed.',
    })

    const [requestUrl, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/deviations/dev-1/timeline',
    )
    expect(requestInit.method).toBe('POST')
    expect(requestInit.body).toBe(JSON.stringify({ note: 'Follow-up completed.' }))
  })

  it('maps backend timeline entries into frontend timeline items', () => {
    const deviation = mapDeviationResponseToListItem(
      {
        id: 'dev-1',
        organizationId: 'org-1',
        establishmentId: 'est-1',
        createdByUserId: 'user-1',
        assignedToUserId: 'user-2',
        title: 'Walk-in fridge too warm',
        description: 'Opening check measured 10C.',
        status: 'IN_PROGRESS',
        severity: 'HIGH',
        category: 'TEMPERATURE',
        createdAt: '2026-04-06T08:00:00Z',
        updatedAt: '2026-04-06T08:05:00Z',
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
            actorUserId: 'user-2',
            authorName: 'Manager User',
            note: 'Products moved to backup cooling.',
            occurredAt: '2026-04-06T08:05:00Z',
          },
        ],
      },
      {
        'user-1': 'Reporter User',
        'user-2': 'Manager User',
      },
    )

    expect(deviation.assignedTo).toEqual(['Manager User'])
    expect(deviation.timeline).toEqual([
      {
        id: 'evt-1',
        createdAt: '2026-04-06T08:00:00Z',
        authorName: 'Reporter User',
        note: 'Deviation reported.',
      },
      {
        id: 'evt-2',
        createdAt: '2026-04-06T08:05:00Z',
        authorName: 'Manager User',
        note: 'Products moved to backup cooling.',
      },
    ])
  })
})
