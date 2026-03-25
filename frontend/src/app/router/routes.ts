import type { RouteRecordRaw } from 'vue-router'

import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'
import LoginPage from '@/auth/pages/LoginPage.vue'
import LandingPage from '@/marketing/pages/LandingPage.vue'
import WorkspaceHomePage from '@/workspace/pages/WorkspaceHomePage.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: PublicLayout,
    children: [
      {
        path: '',
        name: 'landing',
        component: LandingPage,
      },
    ],
  },
  {
    path: '/',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        name: 'login',
        component: LoginPage,
        meta: {
          requiresGuest: true,
        },
      },
    ],
  },
  {
    path: '/app',
    component: AppLayout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        name: 'workspace-home',
        component: WorkspaceHomePage,
      },
    ],
  },
]
