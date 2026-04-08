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

const getCsrfHeadersMock = vi.fn()
const clearCsrfTokenMock = vi.fn()

vi.mock('@/shared/api/csrf', () => ({
  getCsrfHeaders: getCsrfHeadersMock,
  clearCsrfToken: clearCsrfTokenMock,
}))

describe('auth.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    clearCsrfTokenMock.mockReset()
    getCsrfHeadersMock.mockResolvedValue({
      'X-XSRF-TOKEN': 'csrf-token',
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('sends the csrf header when logging in', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        user: { id: 'user-1', email: 'alice@example.com', firstName: 'Alice', lastName: 'Example' },
        accessToken: 'access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        appContext: null,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    const { login } = await import('@/auth/api/auth.api')
    await login({
      email: 'alice@example.com',
      password: 'password123',
    })

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(requestInit.credentials).toBe('include')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('sends the csrf header when refreshing the session', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        user: { id: 'user-1', email: 'alice@example.com', firstName: 'Alice', lastName: 'Example' },
        accessToken: 'access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        appContext: null,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    const { refreshSession } = await import('@/auth/api/auth.api')
    await refreshSession()

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(requestInit.method).toBe('POST')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('sends the csrf header when logging out', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    const { logout } = await import('@/auth/api/auth.api')
    await logout()

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(requestInit.method).toBe('POST')
    expect(requestInit.credentials).toBe('include')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('retries logout once after a csrf rejection', async () => {
    const fetchMock = fetch as Mock
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Access denied' }), {
        status: 403,
        headers: {
          'Content-Type': 'application/json',
        },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    const { logout } = await import('@/auth/api/auth.api')
    await logout()

    expect(clearCsrfTokenMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
