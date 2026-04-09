import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import DocumentsPage from '@/documents/pages/DocumentsPage.vue'

function createDocument(overrides: Partial<Record<string, unknown>> = {}) {
  return {
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
    ...overrides,
  }
}

const {
  listAllEstablishmentDocumentsMock,
  downloadDocumentFileMock,
  deleteDocumentMock,
  authStoreMock,
  appEnvMock,
  routeState,
  routerPushMock,
} = vi.hoisted(() => ({
  listAllEstablishmentDocumentsMock: vi.fn(),
  downloadDocumentFileMock: vi.fn(),
  deleteDocumentMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
    },
    user: {
      globalRoles: [],
    },
  },
  appEnvMock: {
    mode: 'test',
    isDevelopment: true,
    isProduction: false,
    apiBaseUrl: 'http://localhost:8080',
    defaultOrganizationId: undefined as string | undefined,
    defaultEstablishmentId: undefined as string | undefined,
    showDevLoginHint: false,
  },
  routeState: {
    name: 'ik-alkohol-documents',
  },
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/documents/api/documents.api', () => ({
  deleteDocument: deleteDocumentMock,
  downloadDocumentFile: downloadDocumentFileMock,
  listAllEstablishmentDocuments: listAllEstablishmentDocumentsMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

function mountPage() {
  return mount(DocumentsPage, {
    global: {
      stubs: {
        BaseButton: {
          template: '<button><slot /></button>',
        },
      },
    },
  })
}

describe('DocumentsPage', () => {
  afterEach(() => {
    listAllEstablishmentDocumentsMock.mockReset()
    downloadDocumentFileMock.mockReset()
    deleteDocumentMock.mockReset()
    routerPushMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
    }
    authStoreMock.user = {
      globalRoles: [],
    }
    appEnvMock.isDevelopment = true
    appEnvMock.isProduction = false
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined
    routeState.name = 'ik-alkohol-documents'
  })

  it('loads alcohol documents and shows the upload action', async () => {
    listAllEstablishmentDocumentsMock.mockResolvedValue([createDocument()])

    const wrapper = mountPage()
    await flushPromises()

    expect(listAllEstablishmentDocumentsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })
    expect(wrapper.text()).toContain('Important documents')
    expect(wrapper.text()).toContain('Alcohol service licence')
    expect(wrapper.text()).toContain('Upload new document')
    expect(wrapper.text()).toContain('Download')
    expect(wrapper.text()).toContain('Delete')
  })

  it('loads ik-mat documents from the shared page with an upload action', async () => {
    routeState.name = 'ik-mat-documents'
    listAllEstablishmentDocumentsMock.mockResolvedValue([
      createDocument({
        serviceArea: 'IK_MAT',
        title: 'Food safety plan',
        holderName: 'Kitchen team',
        fileName: 'food-safety-plan.pdf',
        status: 'EXPIRED',
      }),
    ])

    const wrapper = mountPage()
    await flushPromises()

    expect(listAllEstablishmentDocumentsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_MAT',
      size: 100,
    })
    expect(wrapper.text()).toContain('Documents')
    expect(wrapper.text()).toContain('Food safety plan')
    expect(wrapper.text()).toContain('Upload new document')
  })

  it('shows download but hides delete and upload for employees', async () => {
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_EMPLOYEE',
    }
    listAllEstablishmentDocumentsMock.mockResolvedValue([createDocument()])

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Download')
    expect(wrapper.text()).not.toContain('Delete')
    expect(wrapper.text()).not.toContain('Upload new document')
  })

  it('deletes a document after confirmation', async () => {
    const confirmMock = vi.spyOn(window, 'confirm').mockReturnValue(true)
    listAllEstablishmentDocumentsMock.mockResolvedValue([createDocument()])
    deleteDocumentMock.mockResolvedValue(undefined)

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.get('.document-action-button-delete').trigger('click')
    await flushPromises()

    expect(deleteDocumentMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      documentId: 'doc-1',
    })
    expect(confirmMock).toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Alcohol service licence')
    confirmMock.mockRestore()
  })
})
