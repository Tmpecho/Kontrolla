import { requestJson } from '@/shared/api/http'

export type BackendStartupStatus = 'STARTING' | 'READY'

export type StartupStatusResponse = {
  status: BackendStartupStatus
  ready: boolean
}

export async function getStartupStatus(): Promise<StartupStatusResponse> {
  return requestJson<StartupStatusResponse>('/api/v1/system/startup-status')
}
