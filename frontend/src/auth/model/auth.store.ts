import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  AuthApiError,
  login as loginRequest,
  refreshSession,
  logout as logoutRequest,
} from '@/auth/api/auth.api'
import type { AuthSession, AuthUser, LoginCredentials } from '@/auth/model/auth.types'

let currentAccessToken: string | null = null

export function getAccessToken(): string | null {
  return currentAccessToken
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const accessToken = ref<string | null>(null)
  const tokenType = ref<string | null>(null)
  const expiresIn = ref<number | null>(null)
  const isSessionReady = ref(false)

  const isAuthenticated = computed(() => user.value !== null && accessToken.value !== null)

  function setSession(session: AuthSession) {
    user.value = session.user
    accessToken.value = session.accessToken
    tokenType.value = session.tokenType
    expiresIn.value = session.expiresIn
    currentAccessToken = session.accessToken
  }

  function clearSession() {
    user.value = null
    accessToken.value = null
    tokenType.value = null
    expiresIn.value = null
    currentAccessToken = null
  }

  async function login(credentials: LoginCredentials) {
    const session = await loginRequest(credentials)
    setSession(session)
    return session
  }

  async function logout() {
    await logoutRequest()
    clearSession()
  }

  async function initializeSession() {
    try {
      const session = await refreshSession()
      setSession(session)
    } catch (error) {
      clearSession()

      if (!(error instanceof AuthApiError) || error.status !== 401) {
        throw error
      }
    } finally {
      isSessionReady.value = true
    }
  }

  return {
    user,
    accessToken,
    tokenType,
    expiresIn,
    isSessionReady,
    isAuthenticated,
    setSession,
    clearSession,
    login,
    logout,
    initializeSession,
  }
})
