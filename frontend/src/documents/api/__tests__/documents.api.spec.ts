import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

import { listEstablishmentDocuments } from '@/documents/api/documents.api'

vi.mock('@/auth/model/auth.store', () => ({
  getAccessToken: () => 'test-access-token',
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

describe('documents.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('requests establishment documents with the service area query', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        items: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listEstablishmentDocuments({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/documents?serviceArea=IK_ALKOHOL&size=100',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })
})
