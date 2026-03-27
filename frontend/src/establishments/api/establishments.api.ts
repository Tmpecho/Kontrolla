import { requestJson } from '@/shared/api/http'
import type { Establishment } from '@/establishments/model/establishment.types'

export async function getEstablishment(
  organizationId: string,
  establishmentId: string,
): Promise<Establishment> {
  return requestJson<Establishment>(
    `/api/v1/organizations/${organizationId}/establishments/${establishmentId}`,
  )
}
