import type { TemperatureLogEntry, TemperatureUnit } from '@/ik-mat/model/temperature.types'
import { requestJson } from '@/shared/api/http'

type TemperatureContext = {
  organizationId: string
  establishmentId: string
}

type CreateTemperatureLogInput = TemperatureContext & {
  temperatureUnitId: string
  temperatureCelsius: number
  measuredAt: string
  note: string | null
}

export async function listTemperatureUnits(
  params: TemperatureContext,
): Promise<TemperatureUnit[]> {
  return requestJson<TemperatureUnit[]>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/temperature-units`,
  )
}

export async function createTemperatureLog(
  params: CreateTemperatureLogInput,
): Promise<TemperatureLogEntry> {
  return requestJson<TemperatureLogEntry>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/temperature-units/${params.temperatureUnitId}/logs`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        temperatureCelsius: params.temperatureCelsius,
        measuredAt: params.measuredAt,
        note: params.note,
      }),
    },
  )
}
