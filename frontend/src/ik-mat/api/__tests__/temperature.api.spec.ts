import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

import {
  createTemperatureLog,
  createTemperatureUnit,
  deleteTemperatureUnit,
  listTemperatureUnits,
} from '@/ik-mat/api/temperature.api'
import { clearCsrfToken } from '@/shared/api/csrf'

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

describe('temperature.api', () => {
  beforeEach(() => {
    clearCsrfToken()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    clearCsrfToken()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('lists temperature units for an establishment', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listTemperatureUnits({
      organizationId: 'org-1',
      establishmentId: 'est-1',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/temperature-units',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })

  it('posts temperature logs as json', async () => {
    const fetchMock = fetch as Mock
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
      new Response(JSON.stringify({
        id: 'log-1',
        measuredAt: '2026-04-09T06:10:00Z',
        temperatureCelsius: 3.2,
        note: 'Opening check completed.',
        loggedByName: 'Maria Nilsen',
      }), {
        status: 201,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await createTemperatureLog({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      temperatureUnitId: 'unit-1',
      temperatureCelsius: 3.2,
      measuredAt: '2026-04-09T06:10:00Z',
      note: 'Opening check completed.',
    })

    expect(fetchMock).toHaveBeenCalledTimes(2)

    const [requestUrl, requestInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/temperature-units/unit-1/logs',
    )
    expect(requestInit.method).toBe('POST')
    expect(new Headers(requestInit.headers).get('Content-Type')).toBe('application/json')
    expect(requestInit.body).toBe(JSON.stringify({
      temperatureCelsius: 3.2,
      measuredAt: '2026-04-09T06:10:00Z',
      note: 'Opening check completed.',
    }))
  })

  it('posts temperature units as json', async () => {
    const fetchMock = fetch as Mock
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
      new Response(JSON.stringify({
        id: 'unit-1',
        name: 'Prep fridge',
        location: 'Main prep line',
        type: 'FRIDGE',
        dueByTime: '08:15:00',
        minimumTemperature: 2,
        maximumTemperature: 4,
        logs: [],
      }), {
        status: 201,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await createTemperatureUnit({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      name: 'Prep fridge',
      location: 'Main prep line',
      type: 'FRIDGE',
      dueByTime: '08:15:00',
      minimumTemperature: 2,
      maximumTemperature: 4,
    })

    expect(fetchMock).toHaveBeenCalledTimes(2)

    const [requestUrl, requestInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/temperature-units',
    )
    expect(requestInit.method).toBe('POST')
    expect(new Headers(requestInit.headers).get('Content-Type')).toBe('application/json')
    expect(requestInit.body).toBe(JSON.stringify({
      name: 'Prep fridge',
      location: 'Main prep line',
      type: 'FRIDGE',
      dueByTime: '08:15:00',
      minimumTemperature: 2,
      maximumTemperature: 4,
    }))
  })

  it('deletes temperature units with the delete endpoint', async () => {
    const fetchMock = fetch as Mock
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
      new Response(null, {
        status: 204,
      }),
    )

    await deleteTemperatureUnit({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      temperatureUnitId: 'unit-1',
    })

    expect(fetchMock).toHaveBeenCalledTimes(2)

    const [requestUrl, requestInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/temperature-units/unit-1',
    )
    expect(requestInit.method).toBe('DELETE')
  })
})
