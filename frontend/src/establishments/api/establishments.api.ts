import type { PageResponse } from '@/checklists/model/checklist.types'
import type { Establishment } from '@/establishments/model/establishment.types'
import { requestJson } from '@/shared/api/http'

type OrganizationContext = {
  organizationId: string
}

export async function listEstablishments(
  params: OrganizationContext & { page?: number; size?: number },
): Promise<PageResponse<Establishment>> {
  return requestJson<PageResponse<Establishment>>(
    `/api/v1/organizations/${params.organizationId}/establishments`,
    {
      query: {
        page: params.page,
        size: params.size,
      },
    },
  )
}
