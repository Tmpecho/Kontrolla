import { buildApiUrl } from '@/shared/config/api'

type CsrfSession = {
  token: string
  headerName: string
  parameterName: string
}

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

let csrfSession: CsrfSession | null = null
let pendingCsrfSession: Promise<CsrfSession> | null = null

export function clearCsrfToken(): void {
  csrfSession = null
  pendingCsrfSession = null
}

export async function ensureCsrfToken(): Promise<CsrfSession> {
  if (csrfSession) {
    return csrfSession
  }

  if (pendingCsrfSession) {
    return pendingCsrfSession
  }

  pendingCsrfSession = fetch(buildApiUrl('/api/v1/auth/csrf'), {
    method: 'GET',
    credentials: 'include',
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error('Failed to initialize CSRF token')
      }

      const payload = (await response.json()) as CsrfSession
      csrfSession = payload
      return payload
    })
    .finally(() => {
      pendingCsrfSession = null
    })

  return pendingCsrfSession
}

export async function getCsrfHeaders(method: string): Promise<Record<string, string>> {
  const normalizedMethod = method.toUpperCase()
  if (!UNSAFE_METHODS.has(normalizedMethod)) {
    return {}
  }

  const session = await ensureCsrfToken()
  return {
    [session.headerName]: session.token,
  }
}
