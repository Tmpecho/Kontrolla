import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/auth/model/auth.store'

const { listEstablishmentsMock } = vi.hoisted(() => ({
  listEstablishmentsMock: vi.fn(),
}))

vi.mock('@/establishments/api/establishments.api', () => ({
  listEstablishments: listEstablishmentsMock,
}))

describe('auth.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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
})
