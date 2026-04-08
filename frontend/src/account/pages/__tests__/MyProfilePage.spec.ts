import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MyProfilePage from '@/account/pages/MyProfilePage.vue'
import { ApiError } from '@/shared/api/http'

const { updateMyProfileMock } = vi.hoisted(() => ({
  updateMyProfileMock: vi.fn(),
}))
let authStoreMock: {
  user: {
    id: string
    email: string
    firstName: string
    lastName: string
    active: boolean
    globalRoles: string[]
    createdAt: string
    updatedAt: string
  } | null
  appContext: {
    organizationId: string
    organizationName: string
    establishmentId: string
    establishmentName: string
  } | null
  setCurrentUser: ReturnType<typeof vi.fn>
}

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/account/api/account.api', () => ({
  updateMyProfile: updateMyProfileMock,
}))

describe('MyProfilePage', () => {
  beforeEach(() => {
    updateMyProfileMock.mockReset()
    authStoreMock = reactive({
      user: {
        id: 'user-1',
        email: 'alice@example.com',
        firstName: 'Alice',
        lastName: 'Example',
        active: true,
        globalRoles: [],
        createdAt: '2026-04-07T08:00:00Z',
        updatedAt: '2026-04-07T08:00:00Z',
      },
      appContext: {
        organizationId: 'org-1',
        organizationName: 'Kontrolla AS',
        establishmentId: 'est-1',
        establishmentName: 'Oslo Restaurant',
      },
      setCurrentUser: vi.fn((nextUser) => {
        authStoreMock.user = nextUser
      }),
    })
  })

  it('renders account details from the auth store', () => {
    const wrapper = mount(MyProfilePage)

    expect(wrapper.text()).toContain('Alice Example')
    expect(wrapper.text()).toContain('alice@example.com')
    expect(wrapper.text()).toContain('Kontrolla AS')
    expect(wrapper.text()).toContain('Oslo Restaurant')
  })

  it('submits trimmed names and updates the auth store on success', async () => {
    updateMyProfileMock.mockResolvedValue({
      ...authStoreMock.user,
      firstName: 'Alicia',
      lastName: 'Example-Smith',
      updatedAt: '2026-04-08T08:00:00Z',
    })

    const wrapper = mount(MyProfilePage)

    const submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()

    await wrapper.get('#profile-first-name').setValue('  Alicia  ')
    await wrapper.get('#profile-last-name').setValue('  Example-Smith  ')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(updateMyProfileMock).toHaveBeenCalledWith({
      firstName: 'Alicia',
      lastName: 'Example-Smith',
    })
    expect(authStoreMock.setCurrentUser).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Profile updated.')
    expect(wrapper.text()).toContain('Alicia Example-Smith')
  })

  it('shows API errors when the profile update fails', async () => {
    updateMyProfileMock.mockRejectedValue(new ApiError('Name update failed.', 400))

    const wrapper = mount(MyProfilePage)

    await wrapper.get('#profile-first-name').setValue('Alicia')
    await wrapper.get('#profile-last-name').setValue('Example-Smith')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(authStoreMock.setCurrentUser).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Name update failed.')
  })
})
