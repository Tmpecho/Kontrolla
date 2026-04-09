import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DeviationFormPage from '@/deviations/pages/DeviationFormPage.vue'
import { ApiError } from '@/shared/api/http'

const {
  createDeviationMock,
  authStoreMock,
  appEnvMock,
  routeState,
  routerPushMock,
} = vi.hoisted(() => ({
  createDeviationMock: vi.fn(),
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
    name: 'ik-mat-deviation-form',
    query: {} as Record<string, unknown>,
  },
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/deviations/api/deviations.api', () => ({
  createDeviation: createDeviationMock,
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

describe('DeviationFormPage', () => {
  afterEach(() => {
    createDeviationMock.mockReset()
    routerPushMock.mockReset()
    routeState.name = 'ik-mat-deviation-form'
    routeState.query = {}
  })

  it('submits trimmed food deviation values and navigates to the IK Mat deviation page', async () => {
    routeState.query = {
      title: 'Temperature deviation - Walk-in fridge',
      category: 'temperature',
      description: 'Prefilled description',
    }
    createDeviationMock.mockResolvedValue({
      id: 'dev-1',
    })

    const wrapper = mount(DeviationFormPage)
    await flushPromises()

    expect((wrapper.get('#category').element as HTMLSelectElement).value).toBe('Temperature')

    await wrapper.get('#title').setValue('  Walk-in fridge too warm  ')
    await wrapper.get('#description').setValue('  Opening check measured 10C.  ')
    await wrapper.get('#severity').setValue('HIGH')

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createDeviationMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      title: 'Walk-in fridge too warm',
      description: 'Opening check measured 10C.',
      category: 'TEMPERATURE',
      severity: 'HIGH',
    })
    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'ik-mat-deviation',
      query: {
        deviationId: 'dev-1',
      },
    })
  })

  it('navigates to the alcohol deviation page on successful alcohol-route submission', async () => {
    routeState.name = 'ik-alkohol-deviation-form'
    routeState.query = {
      category: 'AGE_CONTROL',
    }
    createDeviationMock.mockResolvedValue({
      id: 'dev-alk-1',
    })

    const wrapper = mount(DeviationFormPage)
    await flushPromises()

    expect((wrapper.get('#category').element as HTMLSelectElement).value).toBe('Age control')

    await wrapper.get('#title').setValue('  Missing ID check  ')
    await wrapper.get('#description').setValue('  Guest was not asked for identification.  ')

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createDeviationMock).toHaveBeenCalledWith({
      organizationId: 'org-1',
      establishmentId: 'est-1',
      title: 'Missing ID check',
      description: 'Guest was not asked for identification.',
      category: 'AGE_CONTROL',
      severity: 'MEDIUM',
    })
    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'ik-alkohol-deviation',
      query: {
        deviationId: 'dev-alk-1',
      },
    })
  })

  it('returns to the matching deviation overview when the back button is clicked', async () => {
    routeState.name = 'ik-alkohol-deviation-form'

    const wrapper = mount(DeviationFormPage)
    await flushPromises()

    await wrapper.get('.back-button').trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'ik-alkohol-deviation',
    })
  })

  it('shows the API error message when deviation creation fails', async () => {
    createDeviationMock.mockRejectedValue(new ApiError('Creation failed.', 400))

    const wrapper = mount(DeviationFormPage)
    await flushPromises()

    await wrapper.get('#title').setValue('Deviation title')
    await wrapper.get('#description').setValue('Deviation description')

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(routerPushMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Creation failed.')
  })
})
