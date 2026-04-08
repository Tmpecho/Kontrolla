import type { AuthSession, InviteDetails, LoginCredentials } from '@/auth/model/auth.types'
import { clearCsrfToken, getCsrfHeaders } from '@/shared/api/csrf'
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

async function postWithCsrf(
  path: string,
  options?: {
    body?: BodyInit | null
    headers?: HeadersInit
  },
  retry = true,
): Promise<Response> {
  const headers = new Headers(options?.headers)
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

  if (retry && response.status === 403) {
    clearCsrfToken()
    return postWithCsrf(path, options, false)
  }

  return response
}

async function requestSession(
  path: string,
  options?: {
    body?: string
  },
): Promise<AuthSession> {
  const response = await postWithCsrf(path, {
    body: options?.body,
    headers: {
      'Content-Type': 'application/json',
    },
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
  const response = await postWithCsrf('/api/v1/auth/logout')

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }
}

export async function refreshSession(): Promise<AuthSession> {
  return requestSession('/api/v1/auth/refresh')
}

export async function getInviteDetails(token: string): Promise<InviteDetails> {
  const response = await fetch(buildApiUrl(`/api/v1/auth/invitations/${token}`), {
    method: 'GET',
    credentials: 'include',
  })

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }

  return (await response.json()) as InviteDetails
}

export async function acceptInvite(token: string, password: string): Promise<void> {
  const response = await postWithCsrf(`/api/v1/auth/invitations/${token}/accept`, {
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ password }),
  })

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }
}
