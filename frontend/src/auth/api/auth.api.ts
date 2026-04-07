import type { AuthSession, LoginCredentials } from '@/auth/model/auth.types'
import { getCsrfHeaders } from '@/shared/api/csrf'
import { buildApiUrl } from '@/shared/config/api'

type ApiProblem = {
  detail?: string
  message?: string
}

export class AuthApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'AuthApiError'
    this.status = status
  }
}

async function readProblemMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as ApiProblem
    return payload.message ?? payload.detail ?? 'Request failed'
  } catch {
    return 'Request failed'
  }
}

async function requestSession(
  path: string,
  options?: {
    body?: string
  },
): Promise<AuthSession> {
  const headers = new Headers({
    'Content-Type': 'application/json',
  })

  const csrfHeaders = await getCsrfHeaders('POST')
  for (const [key, value] of Object.entries(csrfHeaders)) {
    headers.set(key, value)
  }

  const response = await fetch(buildApiUrl(path), {
    method: 'POST',
    credentials: 'include',
    headers,
    body: options?.body,
  })

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }

  return (await response.json()) as AuthSession
}

export async function login(credentials: LoginCredentials): Promise<AuthSession> {
  return requestSession('/api/v1/auth/login', {
    body: JSON.stringify(credentials),
  })
}

export async function logout(): Promise<void> {
  const headers = new Headers()
  const csrfHeaders = await getCsrfHeaders('POST')
  for (const [key, value] of Object.entries(csrfHeaders)) {
    headers.set(key, value)
  }

  const response = await fetch(buildApiUrl('/api/v1/auth/logout'), {
    method: 'POST',
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }
}

export async function refreshSession(): Promise<AuthSession> {
  return requestSession('/api/v1/auth/refresh')
}
