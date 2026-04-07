import { describe, expect, it } from 'vitest'

import { toNotificationRoute } from '@/notifications/model/notification.utils'

describe('notification route mapping', () => {
  it('maps deviation notifications to the relevant deviation page', () => {
    expect(toNotificationRoute({
      id: 'notification-1',
      recipientUserId: 'user-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      type: 'DEVIATION_ASSIGNED',
      title: 'Door policy issue',
      message: 'You were assigned this deviation.',
      resourceType: 'DEVIATION',
      resourceId: 'deviation-1',
      createdAt: '2026-04-07T08:00:00Z',
      readAt: null,
      isUnread: true,
    })).toEqual({
      name: 'ik-alkohol-deviation',
      query: {
        deviationId: 'deviation-1',
      },
    })
  })

  it('maps checklist notifications to the checklist page with a selected run id', () => {
    expect(toNotificationRoute({
      id: 'notification-2',
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
      isUnread: true,
    })).toEqual({
      name: 'ik-mat-checklists',
      query: {
        checklistRunId: 'run-1',
      },
    })
  })

  it('falls back to the IK-alkohol dashboard for checklist notifications until that page exists', () => {
    expect(toNotificationRoute({
      id: 'notification-3',
      recipientUserId: 'user-1',
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      type: 'CHECKLIST_ASSIGNED',
      title: 'Alcohol opening checks',
      message: 'You were assigned this checklist run.',
      resourceType: 'CHECKLIST_RUN',
      resourceId: 'run-2',
      createdAt: '2026-04-07T08:00:00Z',
      readAt: null,
      isUnread: true,
    })).toEqual({
      name: 'ik-alkohol-dashboard',
    })
  })
})
