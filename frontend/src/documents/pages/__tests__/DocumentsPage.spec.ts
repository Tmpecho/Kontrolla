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
    auditAssignments: [],
    createdAt: '2026-01-01T08:00:00Z',
    updatedAt: '2026-01-01T08:00:00Z',
    ...overrides,
  }
}

const {
  listAllEstablishmentDocumentsMock,
  downloadDocumentFileMock,
  acknowledgeDocumentReadMock,
  deleteDocumentMock,
  authStoreMock,
  routeState,
  routerPushMock,
} = vi.hoisted(() => ({
  listAllEstablishmentDocumentsMock: vi.fn(),
  downloadDocumentFileMock: vi.fn(),
  acknowledgeDocumentReadMock: vi.fn(),
  deleteDocumentMock: vi.fn(),
  authStoreMock: {
    isStartupPending: false,
    requiresEstablishmentSelection: false,
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
    } as {
      organizationId: string | null
      establishmentId: string | null
      organizationRole: string | null
    } | null,
    user: {
      id: 'user-99',
      globalRoles: [],
    },
  },
  routeState: {
    name: 'ik-alkohol-documents',
  },
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/documents/api/documents.api', () => ({
  acknowledgeDocumentRead: acknowledgeDocumentReadMock,
  deleteDocument: deleteDocumentMock,
  downloadDocumentFile: downloadDocumentFileMock,
  listAllEstablishmentDocuments: listAllEstablishmentDocumentsMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/auth/model/workspace-context', async () => {
  const { computed } = await import('vue')

  return {
    useProtectedWorkspaceContext: () => ({
      organizationId: computed(() => authStoreMock.appContext?.organizationId ?? null),
      establishmentId: computed(() => authStoreMock.appContext?.establishmentId ?? null),
      availableEstablishmentIds: computed(() => {
        const selectedEstablishmentId = authStoreMock.appContext?.establishmentId ?? null
        return selectedEstablishmentId ? [selectedEstablishmentId] : []
      }),
      isStartupPending: computed(() => authStoreMock.isStartupPending),
      requiresEstablishmentSelection: computed(() => authStoreMock.requiresEstablishmentSelection),
      hasOrganizationContext: computed(() => Boolean(authStoreMock.appContext?.organizationId)),
      hasEstablishmentContext: computed(() => {
        return Boolean(
          authStoreMock.appContext?.organizationId && authStoreMock.appContext?.establishmentId,
        )
      }),
      hasAccessibleEstablishmentContext: computed(() => {
        return Boolean(
          authStoreMock.appContext?.organizationId && authStoreMock.appContext?.establishmentId,
        )
      }),
    }),
  }
})

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
    acknowledgeDocumentReadMock.mockReset()
    deleteDocumentMock.mockReset()
    routerPushMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      organizationRole: 'ORG_MANAGER',
    }
    authStoreMock.user = {
      id: 'user-99',
      globalRoles: [],
    }
    authStoreMock.isStartupPending = false
    authStoreMock.requiresEstablishmentSelection = false
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
    expect(wrapper.get('.document-action-button-download').attributes('aria-label')).toBe('Download document')
    expect(wrapper.get('.document-action-button-delete').attributes('aria-label')).toBe('Delete document')
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

    expect(wrapper.get('.document-action-button-download').attributes('aria-label')).toBe('Download document')
    expect(wrapper.find('.document-action-button-delete').exists()).toBe(false)
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

  it('shows and handles the I have read action for an assigned user', async () => {
    listAllEstablishmentDocumentsMock.mockResolvedValue([
      createDocument({
        auditAssignments: [
          {
            userId: 'user-99',
            userEmail: 'reader@example.com',
            userFirstName: 'Reader',
            userLastName: 'One',
            acknowledgedAt: null,
          },
          {
            userId: 'user-100',
            userEmail: 'reader-two@example.com',
            userFirstName: 'Reader',
            userLastName: 'Two',
            acknowledgedAt: '2026-04-09T09:00:00Z',
          },
        ],
      }),
    ])
    acknowledgeDocumentReadMock.mockResolvedValue(
      createDocument({
        auditAssignments: [
          {
            userId: 'user-99',
            userEmail: 'reader@example.com',
            userFirstName: 'Reader',
            userLastName: 'One',
            acknowledgedAt: '2026-04-09T10:15:00Z',
          },
          {
            userId: 'user-100',
            userEmail: 'reader-two@example.com',
            userFirstName: 'Reader',
            userLastName: 'Two',
            acknowledgedAt: '2026-04-09T09:00:00Z',
          },
        ],
      }),
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('I have read')
    expect(wrapper.text()).toContain('1/2 confirmed')
    expect(wrapper.text()).toContain('1 document')
    expect(wrapper.text()).toContain('Awaiting your acknowledgement.')

    await wrapper.get('.document-action-button-acknowledge').trigger('click')
    await flushPromises()

    expect(acknowledgeDocumentReadMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      documentId: 'doc-1',
    })
    expect(wrapper.text()).not.toContain('I have read')
    expect(wrapper.text()).toContain('2/2 confirmed')
    expect(wrapper.text()).toContain('You have confirmed this document.')
  })

  it('shows a neutral context message when an establishment must be selected', async () => {
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: null,
      organizationRole: 'ORG_MANAGER',
    }
    authStoreMock.requiresEstablishmentSelection = true

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Choose an establishment to load documents.')
    expect(wrapper.text()).not.toContain('VITE_DEFAULT_ORGANIZATION_ID')
  })
})
