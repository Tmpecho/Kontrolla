import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import ImportantDocumentsTile from '@/ik-alkohol/components/ImportantDocumentsTile.vue'

function createDocument(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 'doc-1',
    organizationId: 'org-1',
    establishmentId: 'est-1',
    createdByUserId: 'user-1',
    serviceArea: 'IK_ALKOHOL',
    title: 'Responsible service certificate',
    holderName: 'Lina Dahl',
    issueDate: '2026-01-01',
    renewalDate: '2026-05-01',
    fileName: 'responsible-service-certificate.pdf',
    contentType: 'application/pdf',
    fileSizeBytes: 1024,
    status: 'EXPIRING',
    createdAt: '2026-01-01T08:00:00Z',
    updatedAt: '2026-01-01T08:00:00Z',
    ...overrides,
  }
}

const {
  listEstablishmentDocumentsMock,
  authStoreMock,
  appEnvMock,
} = vi.hoisted(() => ({
  listEstablishmentDocumentsMock: vi.fn(),
  authStoreMock: {
    appContext: {
      organizationId: 'org-1',
      establishmentId: 'est-1',
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
}))

vi.mock('@/documents/api/documents.api', () => ({
  listEstablishmentDocuments: listEstablishmentDocumentsMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

describe('ImportantDocumentsTile', () => {
  afterEach(() => {
    listEstablishmentDocumentsMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
    }
  })

  it('loads alcohol documents from the api and shows the next renewal summary', async () => {
    listEstablishmentDocumentsMock.mockResolvedValue({
      items: [
        createDocument({
          id: 'doc-1',
          title: 'Responsible service certificate',
          renewalDate: '2026-05-01',
          status: 'EXPIRING',
        }),
        createDocument({
          id: 'doc-2',
          title: 'Alcohol service licence',
          holderName: 'Oslo Municipality',
          renewalDate: '2026-07-01',
          status: 'VALID',
        }),
      ],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
    })

    const wrapper = mount(ImportantDocumentsTile, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    expect(listEstablishmentDocumentsMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      size: 100,
    })
    expect(wrapper.text()).toContain('Important documents')
    expect(wrapper.text()).toContain('Responsible service certificate')
    expect(wrapper.text()).toContain('0 expired')
    expect(wrapper.text()).toContain('1 expiring within 30 days')
  })
})
