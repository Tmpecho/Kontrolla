import type { AuthSession, LoginCredentials } from '@/auth/model/auth.types'
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

export async function login(credentials: LoginCredentials): Promise<AuthSession> {
  const response = await fetch(buildApiUrl('/api/v1/auth/login'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    throw new AuthApiError(await readProblemMessage(response), response.status)
  }

  return (await response.json()) as AuthSession
}
