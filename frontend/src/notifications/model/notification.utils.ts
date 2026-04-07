import type { RouteLocationRaw } from 'vue-router'

import type {
  NotificationItem,
  NotificationResponse,
  NotificationType,
} from '@/notifications/model/notification.types'

export function mapNotificationResponse(notification: NotificationResponse): NotificationItem {
  return {
    ...notification,
    isUnread: notification.readAt === null,
  }
}

export function formatNotificationTypeLabel(type: NotificationType): string {
  switch (type) {
    case 'CHECKLIST_ASSIGNED':
      return 'Checklist assigned'
    case 'CHECKLIST_OVERDUE':
      return 'Checklist overdue'
    case 'DEVIATION_ASSIGNED':
      return 'Deviation assigned'
    case 'DEVIATION_STATUS_CHANGED':
      return 'Deviation updated'
    case 'DEVIATION_NOTE_ADDED':
      return 'Deviation note'
  }
}

export function toNotificationRoute(notification: NotificationItem): RouteLocationRaw {
  if (notification.resourceType === 'DEVIATION') {
    return {
      name: notification.serviceArea === 'IK_MAT' ? 'ik-mat-deviation' : 'ik-alkohol-deviation',
      query: {
        deviationId: notification.resourceId,
      },
    }
  }

  if (notification.serviceArea === 'IK_MAT') {
    return {
      name: 'ik-mat-checklists',
      query: {
        checklistRunId: notification.resourceId,
      },
    }
  }

  return {
    name: 'ik-alkohol-dashboard',
  }
}
