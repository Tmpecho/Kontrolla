import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const loginMock = vi.fn()
const refreshSessionMock = vi.fn()
const logoutRequestMock = vi.fn()
const clearCsrfTokenMock = vi.fn()

vi.mock('@/auth/api/auth.api', () => ({
  AuthApiError: class AuthApiError extends Error {
    constructor(
      message: string,
      readonly status: number,
    ) {
      super(message)
      this.name = 'AuthApiError'
    }
  },
  login: loginMock,
  refreshSession: refreshSessionMock,
  logout: logoutRequestMock,
}))

vi.mock('@/shared/api/csrf', () => ({
  clearCsrfToken: clearCsrfTokenMock,
}))

describe('auth.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    loginMock.mockReset()
    refreshSessionMock.mockReset()
    logoutRequestMock.mockReset()
    clearCsrfTokenMock.mockReset()
  })

  it('clears the local session after a successful logout', async () => {
    logoutRequestMock.mockResolvedValue(undefined)

    const { useAuthStore } = await import('@/auth/model/auth.store')
    const authStore = useAuthStore()
    authStore.setSession({
      user: {
        id: 'user-1',
        email: 'alice@example.com',
        firstName: 'Alice',
        lastName: 'Example',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-07T08:00:00Z',
        updatedAt: '2026-04-07T08:00:00Z',
      },
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      appContext: null,
    })

    await authStore.logout()

    expect(authStore.isAuthenticated).toBe(false)
    expect(clearCsrfTokenMock).toHaveBeenCalledTimes(1)
  })

  it('still clears the local session when the logout request fails', async () => {
    logoutRequestMock.mockRejectedValue(new Error('Access denied'))

    const { useAuthStore } = await import('@/auth/model/auth.store')
    const authStore = useAuthStore()
    authStore.setSession({
      user: {
        id: 'user-1',
        email: 'alice@example.com',
        firstName: 'Alice',
        lastName: 'Example',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-07T08:00:00Z',
        updatedAt: '2026-04-07T08:00:00Z',
      },
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      appContext: null,
    })

    await authStore.logout()
    expect(authStore.isAuthenticated).toBe(false)
    expect(clearCsrfTokenMock).toHaveBeenCalledTimes(1)
  })
})
