import type {
  DocumentServiceArea,
  EstablishmentDocument,
} from '@/documents/model/document.types'
import { requestJson } from '@/shared/api/http'

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type EstablishmentDocumentsQuery = {
  organizationId: string
  establishmentId: string
  serviceArea: DocumentServiceArea
  page?: number
  size?: number
}

export async function listEstablishmentDocuments(
  params: EstablishmentDocumentsQuery,
): Promise<PageResponse<EstablishmentDocument>> {
  return requestJson<PageResponse<EstablishmentDocument>>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/documents`,
    {
      query: {
        serviceArea: params.serviceArea,
        page: params.page,
        size: params.size,
      },
    },
  )
}
