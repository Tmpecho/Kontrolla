<script setup lang="ts">
import { useAuthStore } from '@/auth/model/auth.store'
import TopBar from '@/app/components/TopBar.vue'

const authStore = useAuthStore()
</script>

<template>
  <div>

    <TopBar/>
    
    <header>
      <div>
        <p>Kontrolla</p>
        <p>Signed-in application</p>
      </div>

      <div v-if="authStore.user">
        <p>{{ authStore.user.firstName }} {{ authStore.user.lastName }}</p>
        <p>{{ authStore.user.email }}</p>
      </div>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>
