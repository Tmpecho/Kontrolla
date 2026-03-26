import { requestJson } from '@/shared/api/http'
import type { ChecklistRun, ChecklistServiceArea, PageResponse } from '@/checklists/model/checklist.types'

type ListChecklistRunsParams = {
  organizationId: string
  establishmentId: string
  serviceArea: ChecklistServiceArea
  page?: number
  size?: number
}

export async function listChecklistRuns(
  params: ListChecklistRunsParams,
): Promise<PageResponse<ChecklistRun>> {
  return requestJson<PageResponse<ChecklistRun>>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/checklists/runs`,
    {
      query: {
        serviceArea: params.serviceArea,
        page: params.page,
        size: params.size,
      },
    },
  )
}
