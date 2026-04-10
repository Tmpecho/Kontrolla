import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

import { clearCsrfToken } from '@/shared/api/csrf'
import {
  listServingHours,
  updateServingHours,
} from '@/establishments/api/serving-hours.api'
import type { ServingHoursDay } from '@/establishments/model/serving-hours.types'

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

describe('serving-hours.api', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    clearCsrfToken()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('lists serving hours for an establishment', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listServingHours({
      organizationId: 'org-1',
      establishmentId: 'est-1',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/serving-hours',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })

  it('updates serving hours as json', async () => {
    const fetchMock = fetch as Mock
    const days: ServingHoursDay[] = [
      {
        dayOfWeek: 'MONDAY',
        closed: false,
        opensAt: '13:00:00',
        closesAt: '22:00:00',
      },
    ]

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({
        token: 'csrf-token',
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(days), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await updateServingHours({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      days,
    })

    expect(fetchMock).toHaveBeenCalledTimes(2)

    const [requestUrl, requestInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/serving-hours',
    )
    expect(requestInit.method).toBe('PUT')
    expect(new Headers(requestInit.headers).get('Content-Type')).toBe('application/json')
    expect(requestInit.body).toBe(JSON.stringify(days))
  })
})
