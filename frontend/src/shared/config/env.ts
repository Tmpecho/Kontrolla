function normalizeBaseUrl(baseUrl?: string): string {
  if (!baseUrl) {
    return ''
  }

  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
}

function isEnabled(value?: string): boolean {
  return value === 'true'
}

export const appEnv = {
  mode: import.meta.env.MODE,
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
  apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
  showDevLoginHint: import.meta.env.DEV && isEnabled(import.meta.env.VITE_SHOW_DEV_LOGIN_HINT),
}
