import { computed } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'

export function useProtectedWorkspaceContext() {
  const authStore = useAuthStore()

  const organizationId = computed(() => authStore.appContext?.organizationId ?? null)
  const establishmentId = computed(() => authStore.appContext?.establishmentId ?? null)
  const availableEstablishmentIds = computed(() => {
    if (establishmentId.value) {
      return [establishmentId.value]
    }

    return authStore.establishments.map((establishment) => establishment.id)
  })

  const isStartupPending = computed(() => authStore.isStartupPending)
  const requiresEstablishmentSelection = computed(() => authStore.requiresEstablishmentSelection)
  const hasOrganizationContext = computed(() => Boolean(organizationId.value))
  const hasEstablishmentContext = computed(() => {
    return Boolean(organizationId.value && establishmentId.value)
  })
  const hasAccessibleEstablishmentContext = computed(() => {
    return Boolean(organizationId.value && availableEstablishmentIds.value.length > 0)
  })

  return {
    organizationId,
    establishmentId,
    availableEstablishmentIds,
    isStartupPending,
    requiresEstablishmentSelection,
    hasOrganizationContext,
    hasEstablishmentContext,
    hasAccessibleEstablishmentContext,
  }
}
