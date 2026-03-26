<script setup lang="ts">
    import { useAuthStore } from '@/auth/model/auth.store'

    const authStore = useAuthStore()
</script>

<template>
<div class="profile-container">
    <p>{{ authStore.user.firstName }} {{ authStore.user.lastName }}</p>
    <p>{{ authStore.user.email }}</p>
</div>
</template>

<style scoped>
    .profile-container {
        position: absolute;
        top: 30px;
        right: 0;
        min-width: 200px;
        padding: 12px;
        background: white;
        border: 1px solid #ddd;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
        z-index: 100;
    }
</style>