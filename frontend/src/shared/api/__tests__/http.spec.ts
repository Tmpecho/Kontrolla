import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

const getCsrfHeadersMock = vi.fn()

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

vi.mock('@/shared/api/csrf', () => ({
  getCsrfHeaders: getCsrfHeadersMock,
}))

describe('shared http client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    getCsrfHeadersMock.mockImplementation(async (method: string) =>
      method === 'GET' ? {} : { 'X-XSRF-TOKEN': 'csrf-token' })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('adds the csrf header to unsafe requests', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    const { requestJson } = await import('@/shared/api/http')
    await requestJson('/api/v1/example', {
      method: 'POST',
      body: JSON.stringify({ hello: 'world' }),
    })

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer test-access-token')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('does not add the csrf header to safe get requests', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ items: [] }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    const { requestJson } = await import('@/shared/api/http')
    await requestJson('/api/v1/example')

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer test-access-token')
    expect(headers.get('X-XSRF-TOKEN')).toBeNull()
  })
})
