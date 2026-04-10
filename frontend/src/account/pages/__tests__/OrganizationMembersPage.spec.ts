import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import OrganizationMembersPage from '@/account/pages/OrganizationMembersPage.vue'

const {
  createManagedOrganizationMemberMock,
  createOrganizationMemberMock,
  listOrganizationMembersMock,
  updateOrganizationMemberMock,
  authStoreMock,
  appEnvMock,
} = vi.hoisted(() => ({
  createManagedOrganizationMemberMock: vi.fn(),
  createOrganizationMemberMock: vi.fn(),
  listOrganizationMembersMock: vi.fn(),
  updateOrganizationMemberMock: vi.fn(),
  authStoreMock: {
    user: {
      id: 'user-admin',
      globalRoles: [],
    },
    appContext: {
      organizationId: 'org-1',
      organizationName: 'Kontrolla Dev Org',
      organizationRole: 'ORG_ADMIN',
      establishmentId: 'est-1',
      establishmentName: 'Restaurant',
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

vi.mock('@/account/api/organization-members.api', () => ({
  createManagedOrganizationMember: createManagedOrganizationMemberMock,
  createOrganizationMember: createOrganizationMemberMock,
  listOrganizationMembers: listOrganizationMembersMock,
  updateOrganizationMember: updateOrganizationMemberMock,
}))

vi.mock('@/auth/model/auth.store', () => ({
  useAuthStore: () => authStoreMock,
}))

vi.mock('@/shared/config/env', () => ({
  appEnv: appEnvMock,
}))

describe('OrganizationMembersPage', () => {
  afterEach(() => {
    createManagedOrganizationMemberMock.mockReset()
    createOrganizationMemberMock.mockReset()
    listOrganizationMembersMock.mockReset()
    updateOrganizationMemberMock.mockReset()
  })

  it('loads active members by default and requests inactive members on demand', async () => {
    listOrganizationMembersMock
      .mockResolvedValueOnce({
        items: [
          {
            id: 'member-1',
            userId: 'user-1',
            userEmail: 'active@example.com',
            userFirstName: 'Active',
            userLastName: 'User',
            role: 'ORG_EMPLOYEE',
            active: true,
            allEstablishments: false,
            establishments: [{ id: 'est-1', name: 'Restaurant' }],
            createdAt: '2026-04-07T10:00:00Z',
            updatedAt: '2026-04-07T10:00:00Z',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 'member-1',
            userId: 'user-1',
            userEmail: 'active@example.com',
            userFirstName: 'Active',
            userLastName: 'User',
            role: 'ORG_EMPLOYEE',
            active: true,
            allEstablishments: false,
            establishments: [{ id: 'est-1', name: 'Restaurant' }],
            createdAt: '2026-04-07T10:00:00Z',
            updatedAt: '2026-04-07T10:00:00Z',
          },
          {
            id: 'member-2',
            userId: 'user-2',
            userEmail: 'inactive@example.com',
            userFirstName: 'Inactive',
            userLastName: 'User',
            role: 'ORG_EMPLOYEE',
            active: false,
            allEstablishments: false,
            establishments: [{ id: 'est-1', name: 'Restaurant' }],
            createdAt: '2026-04-07T11:00:00Z',
            updatedAt: '2026-04-07T11:00:00Z',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 2,
        totalPages: 1,
      })

    const wrapper = mount(OrganizationMembersPage)
    await flushPromises()

    expect(listOrganizationMembersMock).toHaveBeenNthCalledWith(1, {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      includeInactive: false,
      size: 100,
    })
    expect(wrapper.text()).toContain('Organization members')
    expect(wrapper.text()).not.toContain('Entity Management')
    expect(wrapper.text()).not.toContain('inactive@example.com')

    const toggleButton = wrapper
      .findAll('button')
      .find((candidate) => candidate.text() === 'Show inactive')
    await toggleButton?.trigger('click')
    await flushPromises()

    expect(listOrganizationMembersMock).toHaveBeenNthCalledWith(2, {
      organizationId: 'org-1',
      establishmentId: 'est-1',
      includeInactive: true,
      size: 100,
    })
    expect(wrapper.text()).toContain('inactive@example.com')
    expect(wrapper.text()).toContain('1 active, 1 inactive')
  })

  it('shows field-level validation when creating a member without required input', async () => {
    listOrganizationMembersMock.mockResolvedValue({
      items: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 1,
    })

    const wrapper = mount(OrganizationMembersPage)
    await flushPromises()

    const addMemberButton = wrapper
      .findAll('button')
      .find((candidate) => candidate.text() === 'Add member')
    await addMemberButton?.trigger('click')
    await flushPromises()

    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createOrganizationMemberMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Enter an existing user ID.')
  })
})
