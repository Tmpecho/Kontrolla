import type { Router } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'

export function installAuthGuard(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
    const requiresGuest = to.matched.some((record) => record.meta.requiresGuest)
    const requiresMemberManagement = to.matched.some(
      (record) => record.meta.requiresMemberManagement,
    )

    if (requiresAuth && !authStore.isAuthenticated) {
      return { name: 'login' }
    }

    if (requiresGuest && authStore.isAuthenticated) {
      return { name: 'workspace-home' }
    }

    if (requiresMemberManagement) {
      const isPlatformAdmin = authStore.user?.globalRoles.includes('PLATFORM_ADMIN') ?? false
      const canManageMembers =
        isPlatformAdmin ||
        authStore.appContext?.organizationRole === 'ORG_OWNER' ||
        authStore.appContext?.organizationRole === 'ORG_ADMIN'

      if (!canManageMembers) {
        return { name: 'workspace-home' }
      }
    }

    return true
  })
}
