import type {
  TemperatureLogEntry,
  TemperatureUnit,
  TemperatureUnitType,
} from '@/ik-mat/model/temperature.types'
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

type CreateTemperatureUnitInput = TemperatureContext & {
  name: string
  location: string
  type: TemperatureUnitType
  dueByTime: string
  minimumTemperature: number
  maximumTemperature: number
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

export async function createTemperatureUnit(
  params: CreateTemperatureUnitInput,
): Promise<TemperatureUnit> {
  return requestJson<TemperatureUnit>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/temperature-units`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: params.name,
        location: params.location,
        type: params.type,
        dueByTime: params.dueByTime,
        minimumTemperature: params.minimumTemperature,
        maximumTemperature: params.maximumTemperature,
      }),
    },
  )
}

export async function deleteTemperatureUnit(
  params: TemperatureContext & { temperatureUnitId: string },
): Promise<void> {
  await requestJson<void>(
    `/api/v1/organizations/${params.organizationId}/establishments/${params.establishmentId}/temperature-units/${params.temperatureUnitId}`,
    {
      method: 'DELETE',
    },
  )
}
