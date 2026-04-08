import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/auth/model/auth.store'

const {
  loginMock,
  refreshSessionMock,
  logoutRequestMock,
  clearCsrfTokenMock,
  listAdminOrganizationsMock,
  listEstablishmentsMock,
} = vi.hoisted(() => ({
  loginMock: vi.fn(),
  refreshSessionMock: vi.fn(),
  logoutRequestMock: vi.fn(),
  clearCsrfTokenMock: vi.fn(),
  listAdminOrganizationsMock: vi.fn(),
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

vi.mock('@/organizations/api/organizations.api', () => ({
  listAdminOrganizations: listAdminOrganizationsMock,
}))

vi.mock('@/shared/api/csrf', () => ({
  clearCsrfToken: clearCsrfTokenMock,
}))

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return {
    promise,
    resolve,
    reject,
  }
}

describe('auth.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    loginMock.mockReset()
    refreshSessionMock.mockReset()
    logoutRequestMock.mockReset()
    clearCsrfTokenMock.mockReset()
    listAdminOrganizationsMock.mockReset()
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

  it('hydrates admin organizations and selects the stored organization for platform admins', async () => {
    listAdminOrganizationsMock.mockResolvedValue({
      items: [
        {
          id: 'org-1',
          name: 'Alpha Group',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
        {
          id: 'org-2',
          name: 'Beta Group',
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
    listEstablishmentsMock.mockResolvedValue({
      items: [
        {
          id: 'est-2',
          organizationId: 'org-2',
          name: 'Beta Bar',
          type: 'BAR',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    })
    window.localStorage.setItem('kontrolla.organizationSelection', 'org-2')

    const authStore = useAuthStore()
    authStore.setSession({
      user: {
        id: 'admin-1',
        email: 'admin@example.com',
        firstName: 'Admin',
        lastName: 'User',
        active: true,
        globalRoles: ['PLATFORM_ADMIN'],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: null,
    })

    await authStore.hydrateOrganizations()

    expect(authStore.organizations.map((organization) => organization.id)).toEqual(['org-1', 'org-2'])
    expect(authStore.appContext?.organizationId).toBe('org-2')
    expect(authStore.appContext?.organizationName).toBe('Beta Group')
    expect(authStore.establishments.map((establishment) => establishment.id)).toEqual(['est-2'])
  })

  it('switches organization context and rehydrates establishments for platform admins', async () => {
    listEstablishmentsMock.mockResolvedValue({
      items: [
        {
          id: 'est-2',
          organizationId: 'org-2',
          name: 'Beta Bar',
          type: 'BAR',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    })

    const authStore = useAuthStore()
    authStore.setSession({
      user: {
        id: 'admin-1',
        email: 'admin@example.com',
        firstName: 'Admin',
        lastName: 'User',
        active: true,
        globalRoles: ['PLATFORM_ADMIN'],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Alpha Group',
        organizationRole: 'ORG_ADMIN',
        establishmentId: 'est-1',
        establishmentName: 'Alpha Kitchen',
      },
    })
    authStore.organizations = [
      {
        id: 'org-1',
        name: 'Alpha Group',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      {
        id: 'org-2',
        name: 'Beta Group',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
    ]

    await authStore.updateSelectedOrganization('org-2')

    expect(authStore.appContext?.organizationId).toBe('org-2')
    expect(authStore.appContext?.organizationName).toBe('Beta Group')
    expect(authStore.appContext?.organizationRole).toBeNull()
    expect(authStore.appContext?.establishmentId).toBe('est-2')
    expect(window.localStorage.getItem('kontrolla.organizationSelection')).toBe('org-2')
  })

  it('clears stale establishments immediately while switching organizations', async () => {
    const deferred = createDeferred<{
      items: Array<{
        id: string
        organizationId: string
        name: string
        type: 'RESTAURANT' | 'BAR' | 'CAFE' | 'OTHER'
        status: 'ACTIVE' | 'INACTIVE'
        createdAt: string
        updatedAt: string
      }>
      page: number
      size: number
      totalElements: number
      totalPages: number
    }>()

    listEstablishmentsMock.mockReturnValue(deferred.promise)

    const authStore = useAuthStore()
    authStore.setSession({
      user: {
        id: 'admin-1',
        email: 'admin@example.com',
        firstName: 'Admin',
        lastName: 'User',
        active: true,
        globalRoles: ['PLATFORM_ADMIN'],
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Alpha Group',
        organizationRole: 'ORG_ADMIN',
        establishmentId: 'est-1',
        establishmentName: 'Alpha Kitchen',
      },
    })
    authStore.organizations = [
      {
        id: 'org-1',
        name: 'Alpha Group',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
      {
        id: 'org-2',
        name: 'Beta Group',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
    ]
    authStore.establishments = [
      {
        id: 'est-1',
        organizationId: 'org-1',
        name: 'Alpha Kitchen',
        type: 'RESTAURANT',
        status: 'ACTIVE',
        createdAt: '2026-04-08T08:00:00Z',
        updatedAt: '2026-04-08T08:00:00Z',
      },
    ]

    const switchPromise = authStore.updateSelectedOrganization('org-2')

    expect(authStore.appContext?.organizationId).toBe('org-2')
    expect(authStore.appContext?.establishmentId).toBeNull()
    expect(authStore.establishments).toEqual([])

    deferred.resolve({
      items: [
        {
          id: 'est-2',
          organizationId: 'org-2',
          name: 'Beta Bar',
          type: 'BAR',
          status: 'ACTIVE',
          createdAt: '2026-04-08T08:00:00Z',
          updatedAt: '2026-04-08T08:00:00Z',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    })

    await switchPromise

    expect(authStore.appContext?.establishmentId).toBe('est-2')
    expect(authStore.establishments.map((establishment) => establishment.id)).toEqual(['est-2'])
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
