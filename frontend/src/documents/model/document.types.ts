export type DocumentServiceArea = 'IK_MAT' | 'IK_ALKOHOL'

export type DocumentStatus = 'VALID' | 'EXPIRING' | 'EXPIRED'

export type DocumentPreview = {
  id: string
  title: string
  holderName: string
  issueDate: string
  renewalDate: string
}

export type DocumentListItem = DocumentPreview & {
  status: DocumentStatus
}

export type EstablishmentDocument = DocumentListItem & {
  organizationId: string
  establishmentId: string
  createdByUserId: string
  serviceArea: DocumentServiceArea
  fileName: string
  contentType: string
  fileSizeBytes: number
  createdAt: string
  updatedAt: string
}
