import type { RouteRecordRaw } from 'vue-router'

import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'
import LoginPage from '@/auth/pages/LoginPage.vue'
import LandingPage from '@/marketing/pages/LandingPage.vue'
import PriceOfferPage from '@/marketing/pages/PriceOfferPage.vue'
import NotFoundPage from '@/shared/pages/NotFoundPage.vue'
import WorkspaceHomePage from '@/workspace/pages/WorkspaceHomePage.vue'
import IKMatDashboardPage from '@/ik-mat/pages/IKMatDashboardPage.vue'
import IKMatDocumentsPage from '@/ik-mat/pages/IKMatDocumentsPage.vue'
import IKAlkoholDashboardPage from '@/ik-alkohol/pages/IKAlkoholDashboardPage.vue'
import IKAlkoholDocumentsPage from '@/ik-alkohol/pages/IKAlkoholDocumentsPage.vue'
import IKAlkoholDocumentUploadPage from '@/ik-alkohol/pages/IKAlkoholDocumentUploadPage.vue'
import DeviationPage from '@/deviations/pages/DeviationPage.vue'
import DeviationFormPage from '@/deviations/pages/DeviationFormPage.vue'
import MyProfilePage from '@/account/pages/MyProfilePage.vue'
import SettingsPage from '@/account/pages/SettingsPage.vue'

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
      {
        path: 'price-offer',
        name: 'price-offer',
        component: PriceOfferPage,
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundPage,
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
        path: 'ik-mat/deviation',
        name: 'ik-mat-deviation',
        component: DeviationPage,
      },
      {
        path: 'ik-mat/deviation/form',
        name: 'ik-mat-deviation-form',
        component: DeviationFormPage,
      },
      {
        path: 'ik-mat/documents',
        name: 'ik-mat-documents',
        component: IKMatDocumentsPage,
      },
      {
        path: 'ik-alkohol',
        name: 'ik-alkohol-dashboard',
        component: IKAlkoholDashboardPage,
      },
      {
        path: 'ik-alkohol/deviation',
        name: 'ik-alkohol-deviation',
        component: DeviationPage,
      },
      {
        path: 'ik-alkohol/deviation/form',
        name: 'ik-alkohol-deviation-form',
        component: DeviationFormPage,
      },
      {
        path: 'ik-alkohol/documents',
        name: 'ik-alkohol-documents',
        component: IKAlkoholDocumentsPage,
      },
      {
        path: 'ik-alkohol/documents/upload',
        name: 'ik-alkohol-documents-upload',
        component: IKAlkoholDocumentUploadPage,
      },
      {
        path: 'my-profile',
        name: 'my-profile',
        component: MyProfilePage,
      },
      {
        path: 'settings',
        name: 'settings',
        component: SettingsPage,
      },
      {
        path: ':pathMatch(.*)*',
        name: 'app-not-found',
        component: NotFoundPage,
      },
    ],
  },
]
