import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

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

describe('csrf api client', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('fetches the csrf token bootstrap payload', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
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

    const { ensureCsrfToken } = await import('@/shared/api/csrf')
    const csrfSession = await ensureCsrfToken()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/auth/csrf',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
    expect(csrfSession).toEqual({
      token: 'csrf-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    })
  })

  it('caches the csrf token after the first request', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
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

    const { ensureCsrfToken } = await import('@/shared/api/csrf')

    await ensureCsrfToken()
    await ensureCsrfToken()

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
