import { createRouter, createWebHistory } from 'vue-router'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import LoginPage from '@/auth/pages/LoginPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AuthLayout,
      children: [
        {
          path: '',
          redirect: { name: 'login' },
        },
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
