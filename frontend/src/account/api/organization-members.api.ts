import { requestJson } from '@/shared/api/http'
import type {
  CreateManagedOrganizationMemberInput,
  CreateOrganizationMembershipInput,
  ManagedOrganizationMemberProvision,
  OrganizationMembership,
  OrganizationMembershipPage,
  UpdateOrganizationMembershipInput,
} from '@/account/model/organization-members.types'

type OrganizationContext = {
  organizationId: string
}

const getBaseUrl = ({ organizationId }: OrganizationContext) =>
  `/api/v1/organizations/${organizationId}/members`

export async function listOrganizationMembers(
  params: OrganizationContext & { page?: number; size?: number },
): Promise<OrganizationMembershipPage> {
  return requestJson<OrganizationMembershipPage>(getBaseUrl(params), {
    query: {
      page: params.page,
      size: params.size,
    },
  })
}

export async function createOrganizationMember(
  params: OrganizationContext,
  input: CreateOrganizationMembershipInput,
): Promise<OrganizationMembership> {
  return requestJson<OrganizationMembership>(getBaseUrl(params), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export async function createManagedOrganizationMember(
  params: OrganizationContext,
  input: CreateManagedOrganizationMemberInput,
): Promise<ManagedOrganizationMemberProvision> {
  return requestJson<ManagedOrganizationMemberProvision>(`${getBaseUrl(params)}/managed-users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}

export async function updateOrganizationMember(
  params: OrganizationContext & { membershipId: string },
  input: UpdateOrganizationMembershipInput,
): Promise<OrganizationMembership> {
  return requestJson<OrganizationMembership>(`${getBaseUrl(params)}/${params.membershipId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  })
}
