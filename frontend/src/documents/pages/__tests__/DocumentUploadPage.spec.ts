import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import DocumentUploadPage from '@/documents/pages/DocumentUploadPage.vue'

const {
  createDocumentMock,
  authStoreMock,
  appEnvMock,
  routeState,
  routerPushMock,
} = vi.hoisted(() => ({
  createDocumentMock: vi.fn(),
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
  routeState: {
    name: 'ik-alkohol-documents-upload',
  },
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/documents/api/documents.api', () => ({
  createDocument: createDocumentMock,
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

describe('DocumentUploadPage', () => {
  afterEach(() => {
    createDocumentMock.mockReset()
    routerPushMock.mockReset()
    authStoreMock.appContext = {
      organizationId: 'org-1',
      establishmentId: 'est-1',
    }
    routeState.name = 'ik-alkohol-documents-upload'
    appEnvMock.isDevelopment = true
    appEnvMock.isProduction = false
    appEnvMock.defaultOrganizationId = undefined
    appEnvMock.defaultEstablishmentId = undefined
  })

  it('submits the upload form and routes back to the documents page', async () => {
    createDocumentMock.mockResolvedValue({
      id: 'doc-1',
    })

    const wrapper = mount(DocumentUploadPage, {
      global: {
        stubs: {
          BaseButton: {
            template: '<button type="submit"><slot /></button>',
          },
        },
      },
    })

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0]?.setValue('Alcohol service licence')
    await textInputs[1]?.setValue('Oslo Municipality')

    const dateInputs = wrapper.findAll('input[type="date"]')
    await dateInputs[0]?.setValue('2026-01-01')
    await dateInputs[1]?.setValue('2026-10-01')

    const fileInput = wrapper.get('input[type="file"]')
    const file = new File(['%PDF-1.7'], 'alcohol-service-licence.pdf', {
      type: 'application/pdf',
    })
    Object.defineProperty(fileInput.element, 'files', {
      configurable: true,
      value: [file],
    })
    await fileInput.trigger('change')

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createDocumentMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      serviceArea: 'IK_ALKOHOL',
      title: 'Alcohol service licence',
      holderName: 'Oslo Municipality',
      issueDate: '2026-01-01',
      renewalDate: '2026-10-01',
      file,
    })
    expect(routerPushMock).toHaveBeenCalledWith({ name: 'ik-alkohol-documents' })
  })

  it('shows a validation message when no file is selected', async () => {
    const wrapper = mount(DocumentUploadPage, {
      global: {
        stubs: {
          BaseButton: {
            template: '<button type="submit"><slot /></button>',
          },
        },
      },
    })

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0]?.setValue('Alcohol service licence')
    await textInputs[1]?.setValue('Oslo Municipality')

    const dateInputs = wrapper.findAll('input[type="date"]')
    await dateInputs[0]?.setValue('2026-01-01')
    await dateInputs[1]?.setValue('2026-10-01')

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Choose a PDF file to upload.')
    expect(createDocumentMock).not.toHaveBeenCalled()
  })
})
