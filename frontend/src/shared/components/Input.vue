<script setup lang="ts">
defineProps<{
  id: string
  label: string
  modelValue: string
  type?: string
  hint?: string
}>()

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="input-wrapper">
    <label :for="id" class="carbon-label">{{ label }}</label>
    <div v-if="hint" class="carbon-hint">{{ hint }}</div>
    <input
      :id="id"
      :type="type || 'text'"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      class="input"
      required
    />
  </div>
</template>

<style scoped>
.input-wrapper {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
}

.carbon-label {
  font-size: 0.875rem;
  font-weight: 400;
  color: #161616;
  margin-bottom: 0.375rem;
}

.carbon-hint {
  font-size: 0.75rem;
  color: #525252;
  margin-bottom: 0.5rem;
}

.input {
  background-color: #f4f4f4;
  border: none;
  border-bottom: 1px solid #8d8d8d;
  padding: 0.875rem 1rem;
  font-size: 0.875rem;
  color: #161616;
  width: 100%;
  box-sizing: border-box;
  transition: background-color 0.1s;
}

.input:focus {
  outline: 2px solid #0f62fe;
  outline-offset: -2px;
  border-bottom-color: transparent;
}
</style>
