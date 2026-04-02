export type ImportantDocumentStatus = 'VALID' | 'EXPIRING' | 'EXPIRED'

export type ImportantDocumentRecord = {
  id: string
  title: string
  holderName: string
  issueDate: string
  renewalDate: string
}

export type ImportantDocumentListItem = ImportantDocumentRecord & {
  status: ImportantDocumentStatus
}
