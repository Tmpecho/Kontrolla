import type { DeviationListItem, DeviationServiceArea } from '@/deviations/model/deviation.types'

const baseDeviationsByService: Record<DeviationServiceArea, DeviationListItem[]> = {
  IK_MAT: [
    {
      id: 'ik-mat-1',
      serviceArea: 'IK_MAT',
      title: 'Raw salmon delivery measured above receiving limit',
      reportedAt: '2026-04-01T09:10:00+02:00',
      category: 'Temperature',
      severity: 'CRITICAL',
      status: 'OPEN',
      assignedToUserId: 'mock-user-ik-mat-1',
      assignedTo: ['Nora Johansen', 'Elias Berg'],
      description:
        'The delivery temperature for raw salmon was measured above the internal receiving threshold and requires follow-up with the supplier and kitchen lead.',
      timeline: [
        {
          id: 'ik-mat-1-entry-1',
          createdAt: '2026-04-01T09:18:00+02:00',
          authorName: 'Nora Johansen',
          note: 'Delivery isolated in cold storage pending manager review.',
        },
        {
          id: 'ik-mat-1-entry-2',
          createdAt: '2026-04-01T09:19:00+02:00',
          authorName: 'Nora Johansen',
          note: 'Contacted manager.',
        },
      ],
    },
    {
      id: 'ik-mat-2',
      serviceArea: 'IK_MAT',
      title: 'Allergen labels missing on prepared takeaway sauces',
      reportedAt: '2026-03-31T16:25:00+02:00',
      category: 'Allergen handling',
      severity: 'HIGH',
      status: 'IN_PROGRESS',
      assignedToUserId: 'mock-user-ik-mat-2',
      assignedTo: ['Emil Hansen'],
      description:
        'Prepared takeaway sauces were placed in the front fridge without updated allergen labels after relabeling.',
      timeline: [
        {
          id: 'ik-mat-2-entry-1',
          createdAt: '2026-03-31T16:35:00+02:00',
          authorName: 'Emil Hansen',
          note: 'Affected containers removed from display and sent back to prep.',
        },
      ],
    },
    {
      id: 'ik-mat-3',
      serviceArea: 'IK_MAT',
      title: 'Hand-wash station by prep bench was out of soap',
      reportedAt: '2026-03-31T08:05:00+02:00',
      category: 'Cleaning and hygiene',
      severity: 'MEDIUM',
      status: 'RESOLVED',
      assignedToUserId: 'mock-user-ik-mat-3',
      assignedTo: ['Mina Solberg'],
      description:
        'Soap dispenser at the cold-prep sink was empty during the opening check and was replenished after reporting.',
      timeline: [
        {
          id: 'ik-mat-3-entry-1',
          createdAt: '2026-03-31T08:12:00+02:00',
          authorName: 'Mina Solberg',
          note: 'Dispenser refilled and opening stock added to morning checklist notes.',
        },
      ],
    },
    {
      id: 'ik-mat-4',
      serviceArea: 'IK_MAT',
      title: 'Dry storage container missing product date marking',
      reportedAt: '2026-03-30T13:40:00+02:00',
      category: 'Storage and labeling',
      severity: 'LOW',
      status: 'OPEN',
      assignedToUserId: 'mock-user-ik-mat-4',
      assignedTo: ['Sander Vik'],
      description:
        'One dry storage container with prepared topping mix did not have an updated product date or batch marking.',
      timeline: [
        {
          id: 'ik-mat-4-entry-1',
          createdAt: '2026-03-30T13:52:00+02:00',
          authorName: 'Sander Vik',
          note: 'Container moved aside for verification of prep batch.',
        },
      ],
    },
  ],
  IK_ALKOHOL: [
    {
      id: 'ik-alkohol-1',
      serviceArea: 'IK_ALKOHOL',
      title: 'Age verification was skipped during late bar service',
      reportedAt: '2026-04-01T00:18:00+02:00',
      category: 'Age control',
      severity: 'HIGH',
      status: 'OPEN',
      assignedToUserId: 'mock-user-ik-alkohol-1',
      assignedTo: ['Lina Dahl', 'Jonas Olsen'],
      description:
        'A guest was served before ID verification was completed during a high-traffic period at the bar.',
      timeline: [
        {
          id: 'ik-alkohol-1-entry-1',
          createdAt: '2026-04-01T00:32:00+02:00',
          authorName: 'Lina Dahl',
          note: 'Shift lead informed and CCTV timestamp recorded for review.',
        },
      ],
    },
    {
      id: 'ik-alkohol-2',
      serviceArea: 'IK_ALKOHOL',
      title: 'Refusal incident log was not completed before shift close',
      reportedAt: '2026-03-31T23:10:00+02:00',
      category: 'Documentation and training',
      severity: 'MEDIUM',
      status: 'IN_PROGRESS',
      assignedToUserId: 'mock-user-ik-alkohol-2',
      assignedTo: ['Amalie Nilsen'],
      description:
        'A refusal of service was handled correctly, but the incident note was not entered before the end of the shift.',
      timeline: [
        {
          id: 'ik-alkohol-2-entry-1',
          createdAt: '2026-03-31T23:22:00+02:00',
          authorName: 'Amalie Nilsen',
          note: 'Manager asked for a full written note before the next evening shift.',
        },
      ],
    },
    {
      id: 'ik-alkohol-3',
      serviceArea: 'IK_ALKOHOL',
      title: 'Guests remained in the serving area past licensed hours',
      reportedAt: '2026-03-30T02:18:00+02:00',
      category: 'Serving hours',
      severity: 'CRITICAL',
      status: 'OPEN',
      assignedToUserId: 'mock-user-ik-alkohol-3',
      assignedTo: ['Henrik Moe'],
      description:
        'Closing routines started too late and several guests remained in the serving area after licensed hours.',
      timeline: [
        {
          id: 'ik-alkohol-3-entry-1',
          createdAt: '2026-03-30T02:35:00+02:00',
          authorName: 'Henrik Moe',
          note: 'Closing sequence is being reviewed with the weekend team.',
        },
      ],
    },
    {
      id: 'ik-alkohol-4',
      serviceArea: 'IK_ALKOHOL',
      title: 'Door host briefing on intoxication handling was missed',
      reportedAt: '2026-03-29T18:45:00+02:00',
      category: 'Intoxicated guest',
      severity: 'LOW',
      status: 'RESOLVED',
      assignedToUserId: 'mock-user-ik-alkohol-4',
      assignedTo: ['Maja Berg'],
      description:
        'The incoming door host was not briefed on the shift-specific intoxication escalation routine during handover.',
      timeline: [
        {
          id: 'ik-alkohol-4-entry-1',
          createdAt: '2026-03-29T19:05:00+02:00',
          authorName: 'Maja Berg',
          note: 'Briefing checklist updated and reviewed with all hosts.',
        },
      ],
    },
  ],
}

function cloneDeviation(deviation: DeviationListItem): DeviationListItem {
  return {
    ...deviation,
    assignedTo: [...deviation.assignedTo],
    timeline: deviation.timeline.map((entry) => ({
      ...entry,
    })),
  }
}

export function createDeviationDataset(): Record<DeviationServiceArea, DeviationListItem[]> {
  return {
    IK_MAT: baseDeviationsByService.IK_MAT.map(cloneDeviation),
    IK_ALKOHOL: baseDeviationsByService.IK_ALKOHOL.map(cloneDeviation),
  }
}

export const deviationsByService = createDeviationDataset()
