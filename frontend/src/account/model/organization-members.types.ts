import type { PageResponse } from '@/checklists/model/checklist.types'

export type OrganizationRole = 'ORG_OWNER' | 'ORG_ADMIN' | 'ORG_MANAGER' | 'ORG_EMPLOYEE'

export type OrganizationMembership = {
  id: string
  userId: string
  userEmail: string
  userFirstName: string
  userLastName: string
  role: OrganizationRole
  active: boolean
  allEstablishments: boolean
  establishments: Array<{
    id: string
    name: string
  }>
  createdAt: string
  updatedAt: string
}

export type OrganizationMembershipPage = PageResponse<OrganizationMembership>

export type CreateOrganizationMembershipInput = {
  userId: string
  role: OrganizationRole
  active?: boolean
  allEstablishments?: boolean
  establishmentIds?: string[]
}

export type CreateManagedOrganizationMemberInput = {
  email: string
  firstName: string
  lastName: string
  role: OrganizationRole
  active?: boolean
  allEstablishments?: boolean
  establishmentIds?: string[]
}

export type ManagedOrganizationMemberProvision = {
  membership: OrganizationMembership
  inviteExpiresAt: string
  inviteUrl: string | null
}

export type UpdateOrganizationMembershipInput = {
  role: OrganizationRole
  active: boolean
  allEstablishments?: boolean
  establishmentIds?: string[]
}
