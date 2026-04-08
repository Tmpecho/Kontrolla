import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import LoginPage from '@/auth/pages/LoginPage.vue'

const { routeQuery } = vi.hoisted(() => ({
  routeQuery: {} as Record<string, string>,
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: routeQuery,
  }),
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

vi.mock('@/auth/components/LoginForm.vue', () => ({
  default: {
    template: '<form class="login-form-stub" />',
  },
}))

describe('LoginPage', () => {
  afterEach(() => {
    Object.keys(routeQuery).forEach((key) => {
      delete routeQuery[key]
    })
  })

  it('shows the password updated banner when redirected from settings', () => {
    routeQuery.passwordChanged = '1'

    const wrapper = mount(LoginPage)

    expect(wrapper.text()).toContain('Password updated. Sign in again.')
  })

  it('does not show the password updated banner by default', () => {
    const wrapper = mount(LoginPage)

    expect(wrapper.text()).not.toContain('Password updated. Sign in again.')
  })
})
