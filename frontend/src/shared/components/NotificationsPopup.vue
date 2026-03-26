<script setup lang="ts">
</script>

<template>
<div class="notifications-container">
    <p>
        No Notifications
    </p>
</div>
</template>

<style scoped>
.notifications-container {
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
