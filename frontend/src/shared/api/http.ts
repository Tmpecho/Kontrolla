import { getAccessToken } from '@/auth/model/auth.store'
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

export type BlobResponse = {
  blob: Blob
  headers: Headers
  contentType: string | null
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

async function sendRequest(path: string, options: RequestJsonOptions = {}): Promise<Response> {
  const accessToken = getAccessToken()
  const headers = new Headers(options.headers)

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(buildRequestUrl(path, options.query), {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers,
    body: options.body,
  })

  if (!response.ok) {
    throw new ApiError(await readProblemMessage(response), response.status)
  }

  return response
}

export async function requestJson<T>(path: string, options: RequestJsonOptions = {}): Promise<T> {
  const response = await sendRequest(path, options)

  return (await response.json()) as T
}

export async function requestBlob(
  path: string,
  options: RequestJsonOptions = {},
): Promise<BlobResponse> {
  const response = await sendRequest(path, options)

  return {
    blob: await response.blob(),
    headers: response.headers,
    contentType: response.headers.get('Content-Type'),
  }
}

export async function requestVoid(path: string, options: RequestJsonOptions = {}): Promise<void> {
  await sendRequest(path, options)
}
