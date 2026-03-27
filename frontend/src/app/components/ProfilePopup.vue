<script lang="ts" setup>
import { useAuthStore } from '@/auth/model/auth.store'

const authStore = useAuthStore()
</script>

<template>
<div class="profile-container">
    <p v-if="authStore.user">
        {{ authStore.user.firstName }} {{ authStore.user.lastName }}
    </p>
    <p v-if="authStore.user">
      {{ authStore.user.email }}
    </p>
    <p v-else>Not signed in</p>
</div>
</template>

<style scoped>
.profile-container {
  position: absolute;
  top: 60px;
  right: 0;
  z-index: 100;
  min-width: 200px;
  padding: 12px;
  background: var(--color-container);
  border-radius: 2px;
  box-shadow: var(--shadow-elevated);
}
</style>
