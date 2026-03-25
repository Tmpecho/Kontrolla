import { createRouter, createWebHistory } from 'vue-router'
import { installAuthGuard } from '@/app/guards/auth.guard'
import { routes } from '@/app/router/routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

installAuthGuard(router)

export default router
