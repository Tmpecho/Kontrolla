import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import SettingsPage from '@/account/pages/SettingsPage.vue'
import { ApiError } from '@/shared/api/http'

const { changeMyPasswordMock, logoutMock, routerPushMock } = vi.hoisted(() => ({
  changeMyPasswordMock: vi.fn(),
  logoutMock: vi.fn().mockResolvedValue(undefined),
  routerPushMock: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('@/account/api/account.api', () => ({
  changeMyPassword: changeMyPasswordMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => ({
    logout: logoutMock,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

describe('SettingsPage', () => {
  afterEach(() => {
    changeMyPasswordMock.mockReset()
    logoutMock.mockClear()
    routerPushMock.mockClear()
  })

  it('blocks submission when password confirmation does not match', async () => {
    const wrapper = mount(SettingsPage)

    await wrapper.get('#current-password').setValue('password123')
    await wrapper.get('#new-password').setValue('new-password123')
    await wrapper.get('#confirm-new-password').setValue('different-password123')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(changeMyPasswordMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Password confirmation does not match.')
  })

  it('changes the password, logs out, and redirects to login on success', async () => {
    changeMyPasswordMock.mockResolvedValue(undefined)

    const wrapper = mount(SettingsPage)

    await wrapper.get('#current-password').setValue('password123')
    await wrapper.get('#new-password').setValue('new-password123')
    await wrapper.get('#confirm-new-password').setValue('new-password123')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(changeMyPasswordMock).toHaveBeenCalledWith({
      currentPassword: 'password123',
      newPassword: 'new-password123',
    })
    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'login',
      query: {
        passwordChanged: '1',
      },
    })
  })

  it('shows API errors when the password update fails', async () => {
    changeMyPasswordMock.mockRejectedValue(new ApiError('Current password is incorrect', 400))

    const wrapper = mount(SettingsPage)

    await wrapper.get('#current-password').setValue('wrong-password')
    await wrapper.get('#new-password').setValue('new-password123')
    await wrapper.get('#confirm-new-password').setValue('new-password123')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(logoutMock).not.toHaveBeenCalled()
    expect(routerPushMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Current password is incorrect')
  })
})
