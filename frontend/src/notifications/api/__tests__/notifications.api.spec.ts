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

vi.mock('@/auth/model/auth.store', () => ({
  getAccessToken: () => 'test-access-token',
}))

vi.mock('@/shared/api/csrf', () => ({
  getCsrfHeaders: async (method: string) => (method === 'GET' ? {} : { 'X-XSRF-TOKEN': 'csrf-token' }),
}))

describe('notifications.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('maps notification list responses to notification items', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      items: [
        {
          id: 'notification-1',
          recipientUserId: 'user-1',
          organizationId: 'org-1',
          establishmentId: 'est-1',
          serviceArea: 'IK_MAT',
          type: 'CHECKLIST_ASSIGNED',
          title: 'Morning shift',
          message: 'You were assigned this checklist run.',
          resourceType: 'CHECKLIST_RUN',
          resourceId: 'run-1',
          createdAt: '2026-04-07T08:00:00Z',
          readAt: null,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }), {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
      },
    }))

    const { listNotifications } = await import('@/notifications/api/notifications.api')
    const page = await listNotifications({ status: 'UNREAD', size: 5 })

    expect(page.items).toEqual([
      expect.objectContaining({
        id: 'notification-1',
        isUnread: true,
      }),
    ])

    const [url] = fetchMock.mock.calls[0] as [string]
    expect(url).toContain('/api/v1/notifications?status=UNREAD&size=5')
  })

  it('returns the unread notification count', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ unreadCount: 4 }), {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
      },
    }))

    const { getUnreadNotificationCount } = await import('@/notifications/api/notifications.api')
    await expect(getUnreadNotificationCount()).resolves.toBe(4)
  })

  it('uses unsafe requests for mark-read operations', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      id: 'notification-1',
      recipientUserId: 'user-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      type: 'CHECKLIST_ASSIGNED',
      title: 'Morning shift',
      message: 'You were assigned this checklist run.',
      resourceType: 'CHECKLIST_RUN',
      resourceId: 'run-1',
      createdAt: '2026-04-07T08:00:00Z',
      readAt: '2026-04-07T08:30:00Z',
    }), {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
      },
    }))

    const { markNotificationRead } = await import('@/notifications/api/notifications.api')
    const notification = await markNotificationRead('notification-1')

    const [, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = requestInit.headers as Headers
    expect(requestInit.method).toBe('POST')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(notification.isUnread).toBe(false)
  })
})
