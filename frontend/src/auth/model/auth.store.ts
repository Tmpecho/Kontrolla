import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

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
} from '@/auth/model/auth.types'
import { listEstablishments } from '@/establishments/api/establishments.api'
import type { Establishment } from '@/establishments/model/establishment.types'
import { listAdminOrganizations } from '@/organizations/api/organizations.api'
import type { OrganizationSummary } from '@/organizations/model/organization.types'
import { clearCsrfToken } from '@/shared/api/csrf'

let currentAccessToken: string | null = null
const ESTABLISHMENT_SELECTION_STORAGE_KEY = 'kontrolla.establishmentSelectionByOrganization'
const ORGANIZATION_SELECTION_STORAGE_KEY = 'kontrolla.organizationSelection'

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
  const organizations = ref<OrganizationSummary[]>([])
  const isLoadingOrganizations = ref(false)
  const establishments = ref<Establishment[]>([])
  const isLoadingEstablishments = ref(false)

  const isAuthenticated = computed(() => user.value !== null && accessToken.value !== null)
  const isPlatformAdmin = computed(() => user.value?.globalRoles.includes('PLATFORM_ADMIN') ?? false)
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

  function clearSession() {
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

  async function updateSelectedOrganization(organizationId: string | null) {
    const nextOrganization =
      organizationId === null
        ? null
        : (organizations.value.find((candidate) => candidate.id === organizationId) ?? null)

    if (!nextOrganization) {
      appContext.value = sessionAppContext.value
      establishments.value = []
      writeStoredOrganizationSelection(null)
      await hydrateEstablishments()
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
    await hydrateEstablishments()
  }

  async function synchronizeOrganizationSelection() {
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
      await updateSelectedOrganization(storedOrganizationId)
      return
    }

    if (sessionOrganizationId && hasSessionOrganization) {
      await updateSelectedOrganization(sessionOrganizationId)
      return
    }

    await updateSelectedOrganization(activeOrganizations[0]!.id)
  }

  async function hydrateOrganizations() {
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
      await synchronizeOrganizationSelection()
    } catch {
      organizations.value = []
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

  async function hydrateEstablishments() {
    const organizationId = appContext.value?.organizationId

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

        fetchedEstablishments.push(...page.items)
        totalPages = page.totalPages
        pageNumber += 1
      } while (pageNumber < totalPages)

      establishments.value = fetchedEstablishments
        .filter((establishment) => establishment.status === 'ACTIVE')
        .sort((left, right) => left.name.localeCompare(right.name))
      synchronizeEstablishmentSelection()
    } catch {
      establishments.value = []
    } finally {
      isLoadingEstablishments.value = false
    }
  }

  async function login(credentials: LoginCredentials) {
    const session = await loginRequest(credentials)
    setSession(session)
    await hydrateOrganizations()
    if (!isPlatformAdmin.value) {
      await hydrateEstablishments()
    }
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
      await hydrateOrganizations()
      if (!isPlatformAdmin.value) {
        await hydrateEstablishments()
      }
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
    isLoadingOrganizations,
    isLoadingEstablishments,
    isAuthenticated,
    isPlatformAdmin,
    requiresEstablishmentSelection,
    setSession,
    updateSelectedOrganization,
    updateSelectedEstablishment,
    hydrateOrganizations,
    hydrateEstablishments,
    setCurrentUser,
    clearSession,
    login,
    logout,
    initializeSession,
  }
})
