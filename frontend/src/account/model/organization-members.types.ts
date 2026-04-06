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
  createdAt: string
  updatedAt: string
}

export type OrganizationMembershipPage = PageResponse<OrganizationMembership>

export type CreateOrganizationMembershipInput = {
  userId: string
  role: OrganizationRole
  active?: boolean
}

export type UpdateOrganizationMembershipInput = {
  role: OrganizationRole
  active: boolean
}
