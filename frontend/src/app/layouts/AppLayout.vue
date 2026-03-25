<script setup lang="ts">
import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()
</script>

<template>
  <div>
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

    <nav>
      <RouterLink :to="{ name: 'workspace-home' }">Home</RouterLink>
    </nav>

    <main>
      <RouterView />
    </main>
  </div>
</template>
