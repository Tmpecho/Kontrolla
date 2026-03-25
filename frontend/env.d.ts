/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_SHOW_DEV_LOGIN_HINT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
