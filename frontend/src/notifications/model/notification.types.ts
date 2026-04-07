export type NotificationServiceArea = 'IK_MAT' | 'IK_ALKOHOL'

export type NotificationType =
  | 'CHECKLIST_ASSIGNED'
  | 'CHECKLIST_OVERDUE'
  | 'DEVIATION_ASSIGNED'
  | 'DEVIATION_STATUS_CHANGED'
  | 'DEVIATION_NOTE_ADDED'

export type NotificationResourceType = 'CHECKLIST_RUN' | 'DEVIATION'

export type NotificationStatusFilter = 'ALL' | 'UNREAD'

export type NotificationResponse = {
  id: string
  recipientUserId: string
  organizationId: string
  establishmentId: string
  serviceArea: NotificationServiceArea
  type: NotificationType
  title: string
  message: string
  resourceType: NotificationResourceType
  resourceId: string
  createdAt: string
  readAt: string | null
}

export type NotificationItem = NotificationResponse & {
  isUnread: boolean
}

export type UnreadNotificationCountResponse = {
  unreadCount: number
}
