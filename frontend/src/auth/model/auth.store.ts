import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { getStartupStatus } from '@/app/api/startup.api'
import {
  AuthApiError,
  login as loginRequest,
  refreshSession,
  logout as logoutRequest,
} from '@/auth/api/auth.api'
import type {
  AuthAppContext,
  AuthSession,
  AuthUser,
  LoginCredentials,
  WorkspaceStartupStatus,
} from '@/auth/model/auth.types'
import { listEstablishments } from '@/establishments/api/establishments.api'
import type { Establishment } from '@/establishments/model/establishment.types'
import { listAdminOrganizations } from '@/organizations/api/organizations.api'
import type { OrganizationSummary } from '@/organizations/model/organization.types'
import { clearCsrfToken } from '@/shared/api/csrf'
import { ApiError } from '@/shared/api/http'

let currentAccessToken: string | null = null
const ESTABLISHMENT_SELECTION_STORAGE_KEY = 'kontrolla.establishmentSelectionByOrganization'
const ORGANIZATION_SELECTION_STORAGE_KEY = 'kontrolla.organizationSelection'
const STARTUP_POLL_INTERVAL_MS = 2_000

export function getAccessToken(): string | null {
  return currentAccessToken
}

function readStoredSelectionByOrganization(): Record<string, string> {
  if (typeof window === 'undefined') {
    return {}
  }

  try {
    const storedValue = window.localStorage.getItem(ESTABLISHMENT_SELECTION_STORAGE_KEY)

    if (!storedValue) {
      return {}
    }

    const parsedValue = JSON.parse(storedValue) as Record<string, unknown>

    return Object.fromEntries(
      Object.entries(parsedValue).filter(
        (entry): entry is [string, string] =>
          typeof entry[0] === 'string' && typeof entry[1] === 'string',
      ),
    )
  } catch {
    return {}
  }
}

function writeStoredSelectionByOrganization(selectionByOrganization: Record<string, string>) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(
    ESTABLISHMENT_SELECTION_STORAGE_KEY,
    JSON.stringify(selectionByOrganization),
  )
}

function readStoredOrganizationSelection(): string | null {
  if (typeof window === 'undefined') {
    return null
  }

  const storedValue = window.localStorage.getItem(ORGANIZATION_SELECTION_STORAGE_KEY)?.trim()
  return storedValue ? storedValue : null
}

function writeStoredOrganizationSelection(organizationId: string | null) {
  if (typeof window === 'undefined') {
    return
  }

  if (organizationId) {
    window.localStorage.setItem(ORGANIZATION_SELECTION_STORAGE_KEY, organizationId)
    return
  }

  window.localStorage.removeItem(ORGANIZATION_SELECTION_STORAGE_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const accessToken = ref<string | null>(null)
  const tokenType = ref<string | null>(null)
  const expiresIn = ref<number | null>(null)
  const appContext = ref<AuthAppContext | null>(null)
  const sessionAppContext = ref<AuthAppContext | null>(null)
  const isSessionReady = ref(false)
  const startupStatus = ref<WorkspaceStartupStatus>('idle')
  const startupError = ref<string | null>(null)
  const startupStartedAt = ref<number | null>(null)
  const organizations = ref<OrganizationSummary[]>([])
  const isLoadingOrganizations = ref(false)
  const establishments = ref<Establishment[]>([])
  const isLoadingEstablishments = ref(false)
  let establishmentHydrationRequestId = 0
  let startupRequestId = 0
  let startupPollTimeoutId: number | null = null
  let startupPollResolve: (() => void) | null = null

  const isAuthenticated = computed(() => user.value !== null && accessToken.value !== null)
  const isPlatformAdmin = computed(() => user.value?.globalRoles.includes('PLATFORM_ADMIN') ?? false)
  const isStartupPending = computed(() => {
    return (
      startupStatus.value === 'waiting-for-backend' ||
      startupStatus.value === 'bootstrapping-workspace'
    )
  })
  const requiresEstablishmentSelection = computed(() => {
    return (
      Boolean(appContext.value?.organizationId) &&
      establishments.value.length > 1 &&
      !appContext.value?.establishmentId &&
      !isLoadingEstablishments.value
    )
  })

  function setSession(session: AuthSession) {
    user.value = session.user
    accessToken.value = session.accessToken
    tokenType.value = session.tokenType
    expiresIn.value = session.expiresIn
    appContext.value = session.appContext
    sessionAppContext.value = session.appContext
    currentAccessToken = session.accessToken
  }

  function setCurrentUser(nextUser: AuthUser) {
    user.value = nextUser
  }

  function clearStartupPolling() {
    if (typeof window !== 'undefined' && startupPollTimeoutId !== null) {
      window.clearTimeout(startupPollTimeoutId)
    }

    startupPollTimeoutId = null
    startupPollResolve?.()
    startupPollResolve = null
  }

  function resetStartupState() {
    clearStartupPolling()
    startupStatus.value = 'idle'
    startupError.value = null
    startupStartedAt.value = null
  }

  function clearSession() {
    startupRequestId += 1
    establishmentHydrationRequestId += 1
    resetStartupState()
    user.value = null
    accessToken.value = null
    tokenType.value = null
    expiresIn.value = null
    appContext.value = null
    sessionAppContext.value = null
    organizations.value = []
    isLoadingOrganizations.value = false
    establishments.value = []
    isLoadingEstablishments.value = false
    currentAccessToken = null
  }

  function updateSelectedEstablishment(establishmentId: string | null) {
    const organizationId = appContext.value?.organizationId

    if (!appContext.value || !organizationId) {
      return
    }

    const establishment =
      establishmentId === null
        ? null
        : (establishments.value.find((candidate) => candidate.id === establishmentId) ?? null)

    appContext.value = {
      ...appContext.value,
      establishmentId: establishment?.id ?? null,
      establishmentName: establishment?.name ?? null,
    }

    const storedSelectionByOrganization = readStoredSelectionByOrganization()

    if (establishment) {
      storedSelectionByOrganization[organizationId] = establishment.id
    } else {
      delete storedSelectionByOrganization[organizationId]
    }

    writeStoredSelectionByOrganization(storedSelectionByOrganization)
  }

  async function updateSelectedOrganization(
    organizationId: string | null,
    options: { throwOnError?: boolean } = {},
  ) {
    const nextOrganization =
      organizationId === null
        ? null
        : (organizations.value.find((candidate) => candidate.id === organizationId) ?? null)

    if (!nextOrganization) {
      appContext.value = sessionAppContext.value
      establishments.value = []
      writeStoredOrganizationSelection(null)
      await hydrateEstablishments(options)
      return
    }

    const sessionContextForOrganization =
      sessionAppContext.value?.organizationId === nextOrganization.id ? sessionAppContext.value : null

    appContext.value = {
      organizationId: nextOrganization.id,
      organizationName: nextOrganization.name,
      organizationRole: sessionContextForOrganization?.organizationRole ?? null,
      establishmentId: null,
      establishmentName: null,
    }
    establishments.value = []

    writeStoredOrganizationSelection(nextOrganization.id)
    await hydrateEstablishments(options)
  }

  async function synchronizeOrganizationSelection(options: { throwOnError?: boolean } = {}) {
    if (!isPlatformAdmin.value) {
      organizations.value = []
      return
    }

    const activeOrganizations = organizations.value.filter((organization) => organization.status === 'ACTIVE')
    organizations.value = activeOrganizations

    if (activeOrganizations.length === 0) {
      appContext.value = sessionAppContext.value
      establishments.value = []
      writeStoredOrganizationSelection(null)
      return
    }

    const storedOrganizationId = readStoredOrganizationSelection()
    const hasStoredSelection = activeOrganizations.some(
      (organization) => organization.id === storedOrganizationId,
    )
    const sessionOrganizationId = sessionAppContext.value?.organizationId
    const hasSessionOrganization = activeOrganizations.some(
      (organization) => organization.id === sessionOrganizationId,
    )

    if (storedOrganizationId && hasStoredSelection) {
      await updateSelectedOrganization(storedOrganizationId, options)
      return
    }

    if (sessionOrganizationId && hasSessionOrganization) {
      await updateSelectedOrganization(sessionOrganizationId, options)
      return
    }

    await updateSelectedOrganization(activeOrganizations[0]!.id, options)
  }

  async function hydrateOrganizations(options: { throwOnError?: boolean } = {}) {
    if (!isPlatformAdmin.value) {
      organizations.value = []
      return
    }

    isLoadingOrganizations.value = true

    try {
      const fetchedOrganizations: OrganizationSummary[] = []
      let pageNumber = 0
      let totalPages = 1

      do {
        const page = await listAdminOrganizations({
          page: pageNumber,
          size: 100,
        })

        fetchedOrganizations.push(...page.items)
        totalPages = page.totalPages
        pageNumber += 1
      } while (pageNumber < totalPages)

      organizations.value = fetchedOrganizations.sort((left, right) => left.name.localeCompare(right.name))
      await synchronizeOrganizationSelection(options)
    } catch (error) {
      organizations.value = []
      if (options.throwOnError) {
        throw error
      }
    } finally {
      isLoadingOrganizations.value = false
    }
  }

  function synchronizeEstablishmentSelection() {
    const organizationId = appContext.value?.organizationId

    if (!organizationId || !appContext.value) {
      return
    }

    const storedSelectionByOrganization = readStoredSelectionByOrganization()
    const storedEstablishmentId = storedSelectionByOrganization[organizationId]
    const hasStoredSelection = establishments.value.some(
      (establishment) => establishment.id === storedEstablishmentId,
    )

    if (storedEstablishmentId && hasStoredSelection) {
      updateSelectedEstablishment(storedEstablishmentId)
      return
    }

    if (establishments.value.length === 1) {
      updateSelectedEstablishment(establishments.value[0]!.id)
      return
    }

    appContext.value = {
      ...appContext.value,
      establishmentId: null,
      establishmentName: null,
    }
  }

  async function hydrateEstablishments(options: { throwOnError?: boolean } = {}) {
    const organizationId = appContext.value?.organizationId
    const requestId = ++establishmentHydrationRequestId

    if (!organizationId) {
      establishments.value = []
      return
    }

    isLoadingEstablishments.value = true

    try {
      const fetchedEstablishments: Establishment[] = []
      let pageNumber = 0
      let totalPages = 1

      do {
        const page = await listEstablishments({
          organizationId,
          page: pageNumber,
          size: 100,
        })

        if (requestId !== establishmentHydrationRequestId || appContext.value?.organizationId !== organizationId) {
          return
        }

        fetchedEstablishments.push(...page.items)
        totalPages = page.totalPages
        pageNumber += 1
      } while (pageNumber < totalPages)

      if (requestId !== establishmentHydrationRequestId || appContext.value?.organizationId !== organizationId) {
        return
      }

      establishments.value = fetchedEstablishments
        .filter((establishment) => establishment.status === 'ACTIVE')
        .sort((left, right) => left.name.localeCompare(right.name))
      synchronizeEstablishmentSelection()
    } catch (error) {
      if (requestId !== establishmentHydrationRequestId || appContext.value?.organizationId !== organizationId) {
        return
      }

      establishments.value = []
      if (options.throwOnError) {
        throw error
      }
    } finally {
      if (requestId === establishmentHydrationRequestId) {
        isLoadingEstablishments.value = false
      }
    }
  }

  function prepareWorkspaceStartup() {
    startupRequestId += 1
    establishmentHydrationRequestId += 1
    clearStartupPolling()
    startupStatus.value = 'waiting-for-backend'
    startupError.value = null
    startupStartedAt.value = Date.now()
    organizations.value = []
    establishments.value = []
    isLoadingOrganizations.value = false
    isLoadingEstablishments.value = false
    appContext.value = sessionAppContext.value
  }

  function isRetryableStartupPendingError(error: unknown): boolean {
    if (!(error instanceof ApiError)) {
      return true
    }

    return error.status >= 500
  }

  function isNonRecoverableStartupError(error: unknown): boolean {
    return error instanceof ApiError && (error.status === 401 || error.status === 403)
  }

  async function waitForNextStartupPoll(requestId: number) {
    if (typeof window === 'undefined') {
      return
    }

    await new Promise<void>((resolve) => {
      if (requestId !== startupRequestId) {
        resolve()
        return
      }

      startupPollResolve = () => {
        startupPollTimeoutId = null
        startupPollResolve = null
        resolve()
      }

      startupPollTimeoutId = window.setTimeout(() => {
        startupPollResolve?.()
      }, STARTUP_POLL_INTERVAL_MS)
    })
  }

  async function waitForBackendReadiness(requestId: number): Promise<boolean> {
    while (requestId === startupRequestId && isAuthenticated.value) {
      try {
        const backendStartupStatus = await getStartupStatus()

        if (requestId !== startupRequestId) {
          return false
        }

        if (backendStartupStatus.ready) {
          return true
        }
      } catch (error) {
        if (isNonRecoverableStartupError(error)) {
          throw error
        }

        if (!isRetryableStartupPendingError(error)) {
          throw error
        }
      }

      await waitForNextStartupPoll(requestId)
    }

    return false
  }

  async function bootstrapWorkspaceContext(requestId: number) {
    if (requestId !== startupRequestId || !isAuthenticated.value) {
      return
    }

    startupStatus.value = 'bootstrapping-workspace'
    startupError.value = null

    try {
      await hydrateOrganizations({ throwOnError: true })
      if (!isPlatformAdmin.value) {
        await hydrateEstablishments({ throwOnError: true })
      }

      if (requestId !== startupRequestId) {
        return
      }

      startupStatus.value = 'ready'
      startupError.value = null
    } catch (error) {
      if (requestId !== startupRequestId) {
        return
      }

      startupStatus.value = 'error'
      startupError.value =
        error instanceof Error ? error.message : 'Unable to start the workspace.'
    }
  }

  function startWorkspaceStartup() {
    if (!isAuthenticated.value) {
      return
    }

    prepareWorkspaceStartup()
    const requestId = startupRequestId

    void (async () => {
      try {
        const isBackendReady = await waitForBackendReadiness(requestId)

        if (!isBackendReady) {
          return
        }

        await bootstrapWorkspaceContext(requestId)
      } catch (error) {
        if (requestId !== startupRequestId) {
          return
        }

        startupStatus.value = 'error'
        startupError.value =
          error instanceof Error ? error.message : 'Unable to start the workspace.'
      }
    })()
  }

  function retryWorkspaceStartup() {
    startWorkspaceStartup()
  }

  async function login(credentials: LoginCredentials) {
    const session = await loginRequest(credentials)
    setSession(session)
    startWorkspaceStartup()
    return session
  }

  async function logout() {
    try {
      await logoutRequest()
    } catch {
      // Best-effort server logout. Local sign-out should still complete.
    } finally {
      clearCsrfToken()
      clearSession()
    }
  }

  async function initializeSession() {
    try {
      const session = await refreshSession()
      setSession(session)
      startWorkspaceStartup()
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
    appContext,
    organizations,
    establishments,
    isSessionReady,
    startupStatus,
    startupError,
    startupStartedAt,
    isLoadingOrganizations,
    isLoadingEstablishments,
    isAuthenticated,
    isPlatformAdmin,
    isStartupPending,
    requiresEstablishmentSelection,
    setSession,
    updateSelectedOrganization,
    updateSelectedEstablishment,
    hydrateOrganizations,
    hydrateEstablishments,
    startWorkspaceStartup,
    retryWorkspaceStartup,
    setCurrentUser,
    clearSession,
    login,
    logout,
    initializeSession,
  }
})
