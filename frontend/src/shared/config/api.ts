import { appEnv } from '@/shared/config/env'

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  return `${appEnv.apiBaseUrl}${normalizedPath}`
}
