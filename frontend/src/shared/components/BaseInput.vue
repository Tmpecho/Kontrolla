<script setup lang="ts">
defineProps<{
  id: string
  label: string
  modelValue: string
  type?: string
  autocomplete?: string
  placeholder?: string
  hint?: string
  error?: string | null
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

    <textarea
      v-if="type === 'text-area'"
      :id="id"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      :autocomplete="autocomplete"
      :placeholder="placeholder"
      :aria-invalid="Boolean(error)"
      class="input-field textarea-field"
      :class="{ 'input-field-error': Boolean(error) }"
    ></textarea>
    <input
      v-else
      :id="id"
      :type="type || 'text'"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      :autocomplete="autocomplete"
      :placeholder="placeholder"
      :aria-invalid="Boolean(error)"
      class="input-field"
      :class="{ 'input-field-error': Boolean(error) }"
    />
    <p v-if="error" class="input-error">{{ error }}</p>
    <p v-if="hint" class="input-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.input-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 0.5rem;
}

.label-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.input-label {
  font-size: var(--font-size-label);
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: var(--field-label-letter-spacing);
}

.input-field {
  min-height: var(--field-min-height);
  background-color: var(--field-background);
  border: 1px solid var(--field-border-color);
  border-radius: var(--field-radius);
  padding: var(--field-padding-y) var(--field-padding-x);
  font-size: var(--font-size-body);
  color: var(--color-text-primary);
  width: 100%;
  box-sizing: border-box;
}

.input-field:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--field-focus-ring);
}

.input-field-error {
  border-color: var(--color-critical);
}

.input-field-error:focus {
  border-color: var(--color-critical);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-critical) 18%, transparent);
}

.textarea-field {
  min-height: 200px;
  resize: vertical;
}

.input-error,
.input-hint {
  font-size: var(--font-size-body-sm);
  margin: 0;
}

.input-hint {
  color: var(--color-text-secondary);
}

.input-error {
  color: var(--color-critical);
}
</style>
