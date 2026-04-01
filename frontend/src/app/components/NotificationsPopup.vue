<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppPopupShell from '@/app/components/AppPopupShell.vue'

const popupRef = ref<InstanceType<typeof AppPopupShell> | null>(null)

function focusPopup() {
  const popupElement = popupRef.value?.$el as HTMLElement | undefined
  popupElement?.focus()
}

defineExpose({
  focusPopup,
})

onMounted(() => {
  focusPopup()
})
</script>

<template>
  <AppPopupShell
    id="notifications-popup"
    ref="popupRef"
    aria-label="Notifications"
    role="dialog"
  >
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
