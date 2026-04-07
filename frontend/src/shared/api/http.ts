import { getAccessToken } from '@/auth/model/auth.store'
import { getCsrfHeaders } from '@/shared/api/csrf'
import { buildApiUrl } from '@/shared/config/api'

type ApiProblem = {
  detail?: string
  message?: string
}

type QueryValue = boolean | number | string | null | undefined

type RequestJsonOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  query?: Record<string, QueryValue | QueryValue[]>
  body?: BodyInit | null
  headers?: HeadersInit
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function appendQueryParam(searchParams: URLSearchParams, key: string, value: QueryValue): void {
  if (value === null || value === undefined || value === '') {
    return
  }

  searchParams.append(key, String(value))
}

function buildRequestUrl(path: string, query?: Record<string, QueryValue | QueryValue[]>): string {
  if (!query) {
    return buildApiUrl(path)
  }

  const searchParams = new URLSearchParams()

  for (const [key, rawValue] of Object.entries(query)) {
    if (Array.isArray(rawValue)) {
      rawValue.forEach((value) => appendQueryParam(searchParams, key, value))
      continue
    }

    appendQueryParam(searchParams, key, rawValue)
  }

  const queryString = searchParams.toString()
  if (!queryString) {
    return buildApiUrl(path)
  }

  return `${buildApiUrl(path)}?${queryString}`
}

async function readProblemMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as ApiProblem
    return payload.message ?? payload.detail ?? 'Request failed'
  } catch {
    return 'Request failed'
  }
}

export async function requestJson<T>(path: string, options: RequestJsonOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const accessToken = getAccessToken()
  const headers = new Headers(options.headers)

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const csrfHeaders = await getCsrfHeaders(method)
  for (const [key, value] of Object.entries(csrfHeaders)) {
    if (!headers.has(key)) {
      headers.set(key, value)
    }
  }

  const response = await fetch(buildRequestUrl(path, options.query), {
    method,
    credentials: 'include',
    headers,
    body: options.body,
  })

  if (!response.ok) {
    throw new ApiError(await readProblemMessage(response), response.status)
  }

  return (await response.json()) as T
}
