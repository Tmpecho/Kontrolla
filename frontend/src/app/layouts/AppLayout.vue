<script setup lang="ts">
import TopBar from '@/app/components/TopBar.vue'
import Sidebar from '@/app/components/Sidebar.vue'
</script>

<template>
  <div class="app-shell">
    <TopBar />
    <div class="app-body">
      <Sidebar />
      <main class="app-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-surface);
  overflow: hidden;
}

.app-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.app-content {
  flex: 1;
  padding: 24px;
  min-width: 0;
  overflow-y: auto;
}
</style>
