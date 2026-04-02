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

export function parseLocalDate(value: string) {
  const dateOnlyMatch = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)

  if (dateOnlyMatch) {
    const year = Number(dateOnlyMatch[1])
    const month = Number(dateOnlyMatch[2])
    const day = Number(dateOnlyMatch[3])

    return new Date(year, month - 1, day)
  }

  return new Date(value)
}

export function getDocumentStatus(
  documentRecord: ImportantDocumentRecord,
  warningDays = expiryWarningDays,
): ImportantDocumentStatus {
  const today = startOfToday()
  const renewalDate = parseLocalDate(documentRecord.renewalDate)

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
    .sort(
      (left, right) =>
        parseLocalDate(left.renewalDate).getTime() - parseLocalDate(right.renewalDate).getTime(),
    )
}

export function formatDocumentStatus(status: ImportantDocumentStatus) {
  switch (status) {
    case 'VALID':
      return 'Valid'
    case 'EXPIRING':
      return 'Expiring'
    case 'EXPIRED':
      return 'Expired'
    default: {
      const exhaustiveStatus: never = status
      throw new Error(`Unsupported document status: ${String(exhaustiveStatus)}`)
    }
  }
}
