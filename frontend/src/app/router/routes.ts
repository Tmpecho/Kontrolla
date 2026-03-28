import type { RouteRecordRaw } from 'vue-router'

import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'
import LoginPage from '@/auth/pages/LoginPage.vue'
import LandingPage from '@/marketing/pages/LandingPage.vue'
import WorkspaceHomePage from '@/workspace/pages/WorkspaceHomePage.vue'
import IKMatDashboardPage from '@/ik-mat/pages/IKMatDashboardPage.vue'
import IKAlkoholDashboardPage from '@/ik-alkohol/pages/IKAlkoholDashboardPage.vue'
import IKAlkoholDocumentsPage from '@/ik-alkohol/pages/IKAlkoholDocumentsPage.vue'

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
      {
        path: 'ik-mat',
        name: 'ik-mat-dashboard',
        component: IKMatDashboardPage,
      },
      {
        path: 'ik-alkohol',
        name: 'ik-alkohol-dashboard',
        component: IKAlkoholDashboardPage,
      },
      {
        path: 'ik-alkohol/documents',
        name: 'ik-alkohol-documents',
        component: IKAlkoholDocumentsPage,
      },
    ],
  },
]
