<script setup lang="ts">
defineProps<{
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
}>()
</script>

<template>
  <button :type="type || 'button'" :disabled="disabled" class="button">
    <slot></slot>
  </button>
</template>

<style scoped>
.button {
  background-color: var(--color-primary);
  color: var(--color-white);
  border: none;
  padding: 1rem 1rem;
  font-size: 0.875rem;
  font-weight: 400;
  cursor: pointer;
  width: 100%;
  text-align: left;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s ease;
  border-radius: 4px;
}

.button:hover {
  background-color: color-mix(in srgb, var(--color-primary) 88%, black);
}

.button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.button:focus {
  outline: 2px solid var(--color-white);
  outline-offset: -4px;
  box-shadow: 0 0 0 2px var(--color-primary);
}
</style>
