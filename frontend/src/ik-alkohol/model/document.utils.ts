import type {
  ImportantDocumentListItem,
  ImportantDocumentRecord,
  ImportantDocumentStatus,
} from '@/ik-alkohol/model/document.types'

export const expiryWarningDays = 30

export function startOfToday() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today
}

export function getDocumentStatus(
  documentRecord: ImportantDocumentRecord,
  warningDays = expiryWarningDays,
): ImportantDocumentStatus {
  const today = startOfToday()
  const renewalDate = new Date(documentRecord.renewalDate)
  renewalDate.setHours(0, 0, 0, 0)

  if (renewalDate < today) {
    return 'EXPIRED'
  }

  const warningDate = new Date(today)
  warningDate.setDate(warningDate.getDate() + warningDays)

  if (renewalDate <= warningDate) {
    return 'EXPIRING'
  }

  return 'VALID'
}

export function getDocumentsWithStatus(
  documents: ImportantDocumentRecord[],
  warningDays = expiryWarningDays,
): ImportantDocumentListItem[] {
  return documents
    .map((documentRecord) => ({
      ...documentRecord,
      status: getDocumentStatus(documentRecord, warningDays),
    }))
    .sort((left, right) => new Date(left.renewalDate).getTime() - new Date(right.renewalDate).getTime())
}

export function formatDocumentStatus(status: ImportantDocumentStatus) {
  switch (status) {
    case 'VALID':
      return 'Valid'
    case 'EXPIRING':
      return 'Expiring'
    case 'EXPIRED':
      return 'Expired'
  }
}
