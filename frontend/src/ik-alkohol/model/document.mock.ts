import type { ImportantDocumentRecord } from '@/ik-alkohol/model/document.types'

function startOfToday() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today
}

function shiftDate(days: number) {
  const nextDate = startOfToday()
  nextDate.setDate(nextDate.getDate() + days)
  return nextDate
}

function toDateString(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function createDocument(
  id: string,
  title: string,
  holderName: string,
  issueOffsetDays: number,
  renewalOffsetDays: number,
): ImportantDocumentRecord {
  return {
    id,
    title,
    holderName,
    issueDate: toDateString(shiftDate(issueOffsetDays)),
    renewalDate: toDateString(shiftDate(renewalOffsetDays)),
  }
}

export function createImportantDocuments(): ImportantDocumentRecord[] {
  return [
    createDocument(
      'alcohol-licence-main',
      'Alcohol service licence',
      'Oslo Municipality',
      -360,
      185,
    ),
    createDocument(
      'responsible-service-lina',
      'Responsible service certificate',
      'Lina Dahl',
      -520,
      5,
    ),
    createDocument(
      'responsible-service-jonas',
      'Responsible service certificate',
      'Jonas Olsen',
      -470,
      18,
    ),
    createDocument(
      'responsible-service-maja',
      'Responsible service certificate',
      'Maja Berg',
      -430,
      68,
    ),
    createDocument(
      'responsible-service-henrik',
      'Responsible service certificate',
      'Henrik Moe',
      -405,
      82,
    ),
    createDocument(
      'door-host-briefing',
      'Door host competency acknowledgement',
      'Nordic Security AS',
      -140,
      150,
    ),
    createDocument(
      'incident-log-routine',
      'Incident reporting routine sign-off',
      'Shift supervisors',
      -120,
      30,
    ),
    createDocument(
      'cctv-handling',
      'CCTV handling instruction',
      'Venue operations',
      -260,
      210,
    ),
    createDocument(
      'security-agreement',
      'Security provider agreement',
      'Nordic Security AS',
      -310,
      96,
    ),
    createDocument(
      'intoxication-routine',
      'Intoxication handling routine',
      'Bar management',
      -190,
      126,
    ),
    createDocument(
      'age-control-briefing',
      'Age control routine acknowledgement',
      'Bar team',
      -100,
      240,
    ),
    createDocument(
      'fire-safety-coordination',
      'Serving-area fire safety coordination',
      'Building owner',
      -240,
      44,
    ),
    createDocument(
      'closing-routine-approval',
      'Late-night closing routine approval',
      'Operations manager',
      -215,
      72,
    ),
    createDocument(
      'staff-register',
      'Staff permit register',
      'People operations',
      -180,
      -2,
    ),
    createDocument(
      'outdoor-service-addendum',
      'Outdoor service addendum',
      'Venue operations',
      -150,
      58,
    ),
  ]
}
