import type { RouteLocationRaw } from 'vue-router'

import type {
  NotificationItem,
  NotificationResponse,
  NotificationServiceArea,
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
    default:
      return assertNever(type)
  }
}

export function toNotificationRoute(notification: NotificationItem): RouteLocationRaw {
  switch (notification.resourceType) {
    case 'DEVIATION':
      return {
        name: notification.serviceArea === 'IK_MAT' ? 'ik-mat-deviation' : 'ik-alkohol-deviation',
        query: {
          deviationId: notification.resourceId,
        },
      }
    case 'CHECKLIST_RUN':
      return toChecklistRoute(notification.serviceArea, notification.resourceId)
    default:
      return assertNever(notification.resourceType)
  }
}

function toChecklistRoute(serviceArea: NotificationServiceArea, resourceId: string): RouteLocationRaw {
  switch (serviceArea) {
    case 'IK_MAT':
      return {
        name: 'ik-mat-checklists',
        query: {
          checklistRunId: resourceId,
        },
      }
    case 'IK_ALKOHOL':
      return {
        name: 'ik-alkohol-dashboard',
      }
    default:
      return assertNever(serviceArea)
  }
}

function assertNever(value: never): never {
  throw new Error(`Unhandled notification mapping value: ${String(value)}`)
}
