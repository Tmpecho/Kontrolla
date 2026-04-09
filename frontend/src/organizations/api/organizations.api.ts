import type { PageResponse } from '@/checklists/model/checklist.types'
import type { OrganizationSummary } from '@/organizations/model/organization.types'
import { requestJson } from '@/shared/api/http'

export async function listAdminOrganizations(params: {
  page?: number
  size?: number
} = {}): Promise<PageResponse<OrganizationSummary>> {
  return requestJson<PageResponse<OrganizationSummary>>('/api/v1/admin/organizations', {
    query: {
      page: params.page,
      size: params.size,
    },
  })
}
