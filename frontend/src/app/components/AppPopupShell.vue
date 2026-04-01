<script setup lang="ts">
withDefaults(
  defineProps<{
    id?: string
    minWidth?: string
    role?: string
    ariaLabel?: string
    tabIndex?: number
  }>(),
  {
    id: undefined,
    minWidth: '240px',
    role: 'dialog',
    ariaLabel: undefined,
    tabIndex: -1,
  },
)
</script>

<template>
  <div
    :id="id"
    class="popup-shell"
    :style="{ minWidth }"
    :role="role"
    :aria-label="ariaLabel"
    :tabindex="tabIndex"
  >
    <slot />
  </div>
</template>

<style scoped>
.popup-shell {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 100;
  background: var(--color-container);
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}
</style>
