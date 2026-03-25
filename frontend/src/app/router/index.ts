import { createRouter, createWebHistory } from 'vue-router'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'
import LoginPage from '@/auth/pages/LoginPage.vue'
import LandingPage from '@/marketing/pages/LandingPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
        },
      ],
    },
  ],
})

export default router
