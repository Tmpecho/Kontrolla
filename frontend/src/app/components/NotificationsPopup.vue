<script setup lang="ts">
import AppPopupShell from '@/app/components/AppPopupShell.vue'
</script>

<template>
  <AppPopupShell aria-label="Notifications">
    <div class="notifications-container">
      <p>No notifications</p>
    </div>
  </AppPopupShell>
</template>

<style scoped>
.notifications-container {
  padding: 14px 16px;
}

.notifications-container p {
  margin: 0;
}
</style>
