<script setup lang="ts">
defineProps<{
  type?: 'button' | 'submit' | 'reset'
}>()
</script>

<template>
  <button :type="type || 'button'" class="button">
    <slot></slot>
  </button>
</template>

<style scoped>
.button {
  background-color: #0f62fe;
  color: #ffffff;
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
  border-radius: 2px;
}

.button:hover {
  background-color: #0353e9;
}

.button:focus {
  outline: 2px solid #ffffff;
  outline-offset: -4px;
  box-shadow: 0 0 0 2px #0f62fe;
}
</style>
