import { requestJson } from '@/shared/api/http'
import type { Organization } from '@/organizations/model/organization.types'

export async function getOrganization(organizationId: string): Promise<Organization> {
  return requestJson<Organization>(`/api/v1/organizations/${organizationId}`)
}
