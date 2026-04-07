import { requestJson } from '@/shared/api/http'

import type {
  NotificationItem,
  NotificationResponse,
  NotificationStatusFilter,
  UnreadNotificationCountResponse,
} from '@/notifications/model/notification.types'
import { mapNotificationResponse } from '@/notifications/model/notification.utils'
import type { PageResponse } from '@/checklists/model/checklist.types'

export async function listNotifications(params: {
  status?: NotificationStatusFilter
  page?: number
  size?: number
} = {}): Promise<PageResponse<NotificationItem>> {
  const response = await requestJson<PageResponse<NotificationResponse>>('/api/v1/notifications', {
    query: {
      status: params.status,
      page: params.page,
      size: params.size,
    },
  })

  return {
    ...response,
    items: response.items.map(mapNotificationResponse),
  }
}

export async function getUnreadNotificationCount(): Promise<number> {
  const response = await requestJson<UnreadNotificationCountResponse>('/api/v1/notifications/unread-count')
  return response.unreadCount
}

export async function markNotificationRead(notificationId: string): Promise<NotificationItem> {
  const response = await requestJson<NotificationResponse>(`/api/v1/notifications/${notificationId}/read`, {
    method: 'POST',
  })

  return mapNotificationResponse(response)
}

export async function markAllNotificationsRead(): Promise<void> {
  await requestJson<void>('/api/v1/notifications/read-all', {
    method: 'POST',
  })
}
