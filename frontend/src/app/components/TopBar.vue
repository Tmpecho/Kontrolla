<script setup lang="ts">
</script>

<template>
    <div class="top-bar-container">
        <div class="group-container">
            <h1 class="app-title">
                <RouterLink :to="{ name: 'workspace-home' }">Kontrolla</RouterLink>
            </h1>
        </div>
        <div class="group-container">
            <p>Profile</p>
            <p>Settings</p>
        </div>
    </div>
</template>

<style scoped>
    .top-bar-container {
        display: flex;
        flex-direction: row;

        align-items: center;
        justify-content: space-between;
        padding: 0px 30px;

        background-color: #f8f9fa;
    }

    .group-container {
        display: flex;
        flex-direction: row;
        gap: 10px;
    }

    .app-title :deep(a) {
        color: #4a5568;
        text-decoration: none;
        
    }
</style>