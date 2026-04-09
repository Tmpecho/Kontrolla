import type {
  DocumentServiceArea,
  EstablishmentDocument,
} from '@/documents/model/document.types'
import { requestBlob, requestJson, requestVoid } from '@/shared/api/http'

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

type DocumentCreateInput = {
  organizationId: string
  establishmentId: string
  serviceArea: DocumentServiceArea
  title: string
  holderName: string
  issueDate: string
  renewalDate: string
  file: File
}

type DocumentActionParams = {
  organizationId: string
  establishmentId: string
  documentId: string
}

export type DownloadedDocumentFile = {
  blob: Blob
  contentType: string | null
  fileName: string
}

const defaultDocumentPageSize = 100

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

export async function listAllEstablishmentDocuments(
  params: Omit<EstablishmentDocumentsQuery, 'page'>,
): Promise<EstablishmentDocument[]> {
  const size = params.size ?? defaultDocumentPageSize
  const firstPage = await listEstablishmentDocuments({
    ...params,
    page: 0,
    size,
  })
  const items = [...firstPage.items]

  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const nextPage = await listEstablishmentDocuments({
      ...params,
      page,
      size,
    })

    items.push(...nextPage.items)
  }

  return items
}

export async function createDocument(params: DocumentCreateInput): Promise<EstablishmentDocument> {
  const formData = new FormData()

  formData.append(
    'metadata',
    new Blob([
      JSON.stringify({
        serviceArea: params.serviceArea,
        title: params.title,
        holderName: params.holderName,
        issueDate: params.issueDate,
        renewalDate: params.renewalDate,
      }),
    ], {
      type: 'application/json',
    }),
  )
  formData.append('file', params.file)

  return requestJson<EstablishmentDocument>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/documents`,
    {
      method: 'POST',
      body: formData,
    },
  )
}

export async function downloadDocumentFile(
  params: DocumentActionParams,
): Promise<DownloadedDocumentFile> {
  const response = await requestBlob(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/documents/${params.documentId}/file`,
  )

  return {
    blob: response.blob,
    contentType: response.contentType,
    fileName: parseFileName(response.headers.get('Content-Disposition')) ?? 'document.pdf',
  }
}

export async function deleteDocument(params: DocumentActionParams): Promise<void> {
  await requestVoid(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/documents/${params.documentId}`,
    {
      method: 'DELETE',
    },
  )
}

function parseFileName(contentDisposition: string | null): string | null {
  if (!contentDisposition) {
    return null
  }

  const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }

  const fileNameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return fileNameMatch?.[1] ?? null
}
