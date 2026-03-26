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
            <img class="top-bar-img" src="@/assets/icons/notification.png" alt="Notifications"></img>
            <img class="top-bar-img" src="@/assets/icons/settings.png" alt="Settings"></img>
            <img class="top-bar-img" src="@/assets/icons/profile.png" alt="Profile"></img>
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
        gap: 0px 30px;
    }

    .app-title {
        margin: 0px;
    }

    .app-title :deep(a) {
        font-size: 20px;
        color: #4a5568;
        text-decoration: none;   
    }

    .top-bar-img {
        width: 20px;
        height: 20px;
    }
</style>