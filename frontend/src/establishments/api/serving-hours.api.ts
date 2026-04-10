import type { ServingHoursDay } from '@/establishments/model/serving-hours.types'
import { requestJson } from '@/shared/api/http'

type EstablishmentContext = {
  organizationId: string
  establishmentId: string
}

export async function listServingHours(
  params: EstablishmentContext,
): Promise<ServingHoursDay[]> {
  return requestJson<ServingHoursDay[]>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/serving-hours`,
  )
}

export async function updateServingHours(
  params: EstablishmentContext & {
    days: ServingHoursDay[]
  },
): Promise<ServingHoursDay[]> {
  return requestJson<ServingHoursDay[]>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/serving-hours`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(params.days),
    },
  )
}
