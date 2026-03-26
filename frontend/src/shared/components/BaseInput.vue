<script setup lang="ts">
defineProps<{
  id: string
  label: string
  modelValue: string
  type?: string
  autocomplete?: string
  placeholder?: string
  hint?: string
}>()

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="input-container">
    <div class="label-wrapper">
      <label :for="id" class="input-label">{{ label }}</label>
      <div class="aside-content">
        <slot name="aside"></slot>
      </div>
    </div>
    <input
      :id="id"
      :type="type || 'text'"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      :autocomplete="autocomplete"
      :placeholder="placeholder"
      class="input-field"
      required
    />
    <p v-if="hint" class="input-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.input-container {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.label-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.5rem;
}

.input-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.input-field {
  background-color: #f4f4f4;
  border: none;
  border-bottom: 1px solid #a8a8a8;
  border-radius: 3px;
  padding: 0.875rem 0.5rem;
  font-size: 1rem;
  color: #161616;
  width: 100%;
  box-sizing: border-box;
}

.input-field:focus {
  outline: 2px solid #3763f4;
  outline-offset: -2px;
  border-bottom-color: transparent;
}

.input-hint {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0.5rem 0 0 0;
  padding-left: 0.25rem;
}
</style>
