import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

import {
  createDocument,
  deleteDocument,
  downloadDocumentFile,
  listEstablishmentDocuments,
} from '@/documents/api/documents.api'

vi.mock('@/auth/model/auth.store', () => ({
  getAccessToken: () => 'test-access-token',
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: {
    mode: 'test',
    isDevelopment: true,
    isProduction: false,
    apiBaseUrl: 'http://localhost:8080',
    defaultOrganizationId: undefined,
    defaultEstablishmentId: undefined,
    showDevLoginHint: false,
  },
}))

describe('documents.api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('requests establishment documents with the service area query', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        items: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
      }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await listEstablishmentDocuments({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/documents?serviceArea=IK_ALKOHOL&size=100',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })

  it('posts document uploads as multipart form data', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({
        id: 'doc-1',
        organizationId: 'org-1',
        establishmentId: 'est-1',
        createdByUserId: 'user-1',
        serviceArea: 'IK_ALKOHOL',
        title: 'Alcohol service licence',
        holderName: 'Oslo Municipality',
        issueDate: '2026-01-01',
        renewalDate: '2026-10-01',
        fileName: 'alcohol-service-licence.pdf',
        contentType: 'application/pdf',
        fileSizeBytes: 2048,
        status: 'VALID',
        createdAt: '2026-01-01T08:00:00Z',
        updatedAt: '2026-01-01T08:00:00Z',
      }), {
        status: 201,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    )

    await createDocument({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      title: 'Alcohol service licence',
      holderName: 'Oslo Municipality',
      issueDate: '2026-01-01',
      renewalDate: '2026-10-01',
      file: new File(['%PDF-1.7'], 'alcohol-service-licence.pdf', { type: 'application/pdf' }),
    })

    const [requestUrl, requestInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(requestUrl).toBe(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/documents',
    )
    expect(requestInit.method).toBe('POST')
    expect(requestInit.body).toBeInstanceOf(FormData)
  })

  it('downloads a document file from the file endpoint', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(
      new Response('%PDF-1.7', {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="license.pdf"',
        },
      }),
    )

    const file = await downloadDocumentFile({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      documentId: 'doc-1',
    })

    expect(file.fileName).toBe('license.pdf')
    expect(file.contentType).toBe('application/pdf')
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/documents/doc-1/file',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      }),
    )
  })

  it('deletes a document with the delete endpoint', async () => {
    const fetchMock = fetch as Mock
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await deleteDocument({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      documentId: 'doc-1',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/organizations/org-1/establishments/est-1/documents/doc-1',
      expect.objectContaining({
        method: 'DELETE',
        credentials: 'include',
      }),
    )
  })
})
