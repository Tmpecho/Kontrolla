import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import LoginForm from '@/auth/components/LoginForm.vue'

const { authStoreMock, loginMock, routerPushMock } = vi.hoisted(() => ({
  loginMock: vi.fn(),
  routerPushMock: vi.fn().mockResolvedValue(undefined),
  authStoreMock: {
    login: vi.fn(),
  },
}))

authStoreMock.login = loginMock

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

function createDeferred<T>() {
  let resolve: (value: T) => void
  let reject: (error?: unknown) => void

  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return {
    promise,
    resolve: resolve!,
    reject: reject!,
  }
}

describe('LoginForm', () => {
  afterEach(() => {
    loginMock.mockReset()
    routerPushMock.mockReset()
  })

  it('submits credentials and navigates to the workspace on successful login', async () => {
    loginMock.mockResolvedValue({
      accessToken: 'token',
    })

    const wrapper = mount(LoginForm)

    await wrapper.get('#email').setValue('demo@example.com')
    await wrapper.get('#password').setValue('password123')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith({
      email: 'demo@example.com',
      password: 'password123',
    })
    expect(routerPushMock).toHaveBeenCalledWith({ name: 'workspace-home' })
    expect(wrapper.text()).not.toContain('Unable to log in')
  })

  it('shows the returned login error message and does not navigate on failure', async () => {
    loginMock.mockRejectedValue(new Error('Invalid email or password'))

    const wrapper = mount(LoginForm)

    await wrapper.get('#email').setValue('demo@example.com')
    await wrapper.get('#password').setValue('wrong-password')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(routerPushMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Invalid email or password')
  })

  it('shows the submitting state while the login request is in flight', async () => {
    const deferred = createDeferred<{ accessToken: string }>()
    loginMock.mockReturnValue(deferred.promise)

    const wrapper = mount(LoginForm)

    await wrapper.get('#email').setValue('demo@example.com')
    await wrapper.get('#password').setValue('password123')
    await wrapper.get('form').trigger('submit.prevent')
    await nextTick()

    const submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
    expect(submitButton.text()).toContain('Signing In...')

    deferred.resolve({ accessToken: 'token' })
    await flushPromises()

    expect(submitButton.attributes('disabled')).toBeUndefined()
    expect(submitButton.text()).toContain('Sign In')
  })
})
