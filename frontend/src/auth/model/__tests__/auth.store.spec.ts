import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/auth/model/auth.store'

const {
  loginMock,
  refreshSessionMock,
  logoutRequestMock,
  clearCsrfTokenMock,
  listEstablishmentsMock,
} = vi.hoisted(() => ({
  loginMock: vi.fn(),
  refreshSessionMock: vi.fn(),
  logoutRequestMock: vi.fn(),
  clearCsrfTokenMock: vi.fn(),
  listEstablishmentsMock: vi.fn(),
}))

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

vi.mock('@/establishments/api/establishments.api', () => ({
  listEstablishments: listEstablishmentsMock,
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
    listEstablishmentsMock.mockReset()

    const storage = new Map<string, string>()

    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => {
          storage.set(key, value)
        },
        removeItem: (key: string) => {
          storage.delete(key)
        },
      },
    })
  })

  afterEach(() => {
    listEstablishmentsMock.mockReset()
  })

  it('requires an explicit establishment selection when multiple active establishments exist', async () => {
    listEstablishmentsMock.mockResolvedValue({
      items: [
        {
          id: 'est-1',
          organizationId: 'org-1',
          name: 'Kitchen',
          type: 'RESTAURANT',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
        {
          id: 'est-2',
          organizationId: 'org-1',
          name: 'Bar',
          type: 'BAR',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
    })

    const authStore = useAuthStore()

    authStore.setSession({
      user: {
        id: 'user-1',
        email: 'user@example.com',
        firstName: 'Test',
        lastName: 'User',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Org 1',
        organizationRole: 'ORG_MANAGER',
        establishmentId: 'est-1',
        establishmentName: 'Kitchen',
      },
    })

    await authStore.hydrateEstablishments()

    expect(authStore.establishments).toHaveLength(2)
    expect(authStore.appContext?.establishmentId).toBeNull()
    expect(authStore.requiresEstablishmentSelection).toBe(true)
  })

  it('persists a selected establishment for the current organization', async () => {
    listEstablishmentsMock.mockResolvedValue({
      items: [
        {
          id: 'est-1',
          organizationId: 'org-1',
          name: 'Kitchen',
          type: 'RESTAURANT',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
        {
          id: 'est-2',
          organizationId: 'org-1',
          name: 'Bar',
          type: 'BAR',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
    })

    const authStore = useAuthStore()

    authStore.setSession({
      user: {
        id: 'user-1',
        email: 'user@example.com',
        firstName: 'Test',
        lastName: 'User',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Org 1',
        organizationRole: 'ORG_MANAGER',
        establishmentId: 'est-1',
        establishmentName: 'Kitchen',
      },
    })

    await authStore.hydrateEstablishments()
    authStore.updateSelectedEstablishment('est-2')

    expect(authStore.appContext?.establishmentId).toBe('est-2')
    expect(authStore.appContext?.establishmentName).toBe('Bar')
    expect(window.localStorage.getItem('kontrolla.establishmentSelectionByOrganization')).toBe(
      JSON.stringify({ 'org-1': 'est-2' }),
    )
  })

  it('loads every establishment page before synchronizing the selector state', async () => {
    listEstablishmentsMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'est-1',
            organizationId: 'org-1',
            name: 'Kitchen',
            type: 'RESTAURANT',
            status: 'ACTIVE',
            createdAt: '2026-04-08T08:00:00Z',
            updatedAt: '2026-04-08T08:00:00Z',
          },
          {
            id: 'est-2',
            organizationId: 'org-1',
            name: 'Bar',
            type: 'BAR',
            status: 'ACTIVE',
            createdAt: '2026-04-08T08:00:00Z',
            updatedAt: '2026-04-08T08:00:00Z',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 3,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'est-3',
            organizationId: 'org-1',
            name: 'Cafe',
            type: 'CAFE',
            status: 'ACTIVE',
            createdAt: '2026-04-08T08:00:00Z',
            updatedAt: '2026-04-08T08:00:00Z',
          },
        ],
        page: 1,
        size: 100,
        totalElements: 3,
        totalPages: 2,
      })

    const authStore = useAuthStore()

    authStore.setSession({
      user: {
        id: 'user-1',
        email: 'user@example.com',
        firstName: 'Test',
        lastName: 'User',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Org 1',
        organizationRole: 'ORG_MANAGER',
        establishmentId: 'est-1',
        establishmentName: 'Kitchen',
      },
    })

    await authStore.hydrateEstablishments()

    expect(listEstablishmentsMock).toHaveBeenCalledTimes(2)
    expect(listEstablishmentsMock).toHaveBeenNthCalledWith(1, {
      organizationId: 'org-1',
      page: 0,
      size: 100,
    })
    expect(listEstablishmentsMock).toHaveBeenNthCalledWith(2, {
      organizationId: 'org-1',
      page: 1,
      size: 100,
    })
    expect(authStore.establishments.map((establishment) => establishment.id)).toEqual([
      'est-2',
      'est-3',
      'est-1',
    ])
  })

  it('clears the local session after a successful logout', async () => {
    logoutRequestMock.mockResolvedValue(undefined)

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

  it('updates the current user without changing the session tokens', async () => {
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

    authStore.setCurrentUser({
      id: 'user-1',
      email: 'alice@example.com',
      firstName: 'Alicia',
      lastName: 'Example-Smith',
      active: true,
      globalRoles: [],
      createdAt: '2026-04-07T08:00:00Z',
      updatedAt: '2026-04-08T08:00:00Z',
    })

    expect(authStore.user?.firstName).toBe('Alicia')
    expect(authStore.user?.lastName).toBe('Example-Smith')
    expect(authStore.accessToken).toBe('access-token')
    expect(authStore.isAuthenticated).toBe(true)
  })
})
