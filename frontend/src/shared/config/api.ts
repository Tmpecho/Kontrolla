function normalizeBaseUrl(baseUrl?: string): string {
  if (!baseUrl) {
    return ''
  }

  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
}

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const baseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)

  return `${baseUrl}${normalizedPath}`
}
