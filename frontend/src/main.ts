import { createApp } from 'vue'
import { createPinia } from 'pinia'

import { useAuthStore } from '@/auth/model/auth.store'

import App from './App.vue'
import './app/global.css'
import router from './app/router'

const pinia = createPinia()
const app = createApp(App)

async function bootstrap() {
  app.use(pinia)

  const authStore = useAuthStore(pinia)
  try {
    await authStore.initializeSession()
  } catch {
    // If session bootstrap fails, still mount the app and let the user recover.
  }

  app.use(router)
  app.mount('#app')
}

void bootstrap()
