<template>
  <div class="hybrid-checklist" :class="{ 'hybrid-checklist--error': error }">
    <fieldset class="hybrid-checklist__fieldset">
      <legend class="hybrid-checklist__legend">
        <slot name="legend">{{ legend }}</slot>
      </legend>

      <p v-if="hint" class="hybrid-checklist__helper">
        {{ hint }}
      </p>

      <div v-if="error" class="hybrid-checklist__validation">
        <p class="hybrid-checklist__error-text">
          {{ error }}
        </p>
      </div>

      <div class="hybrid-checklist__group">
        <div v-for="item in options" :key="item.id" class="hybrid-checkbox">
          <input
            type="checkbox"
            :id="item.id"
            :value="item.value"
            :checked="modelValue.includes(item.value)"
            :indeterminate.prop="item.indeterminate"
            @change="toggle(item.value)"
            class="hybrid-checkbox__input"
          />
          <label :for="item.id" class="hybrid-checkbox__label">
            <span class="hybrid-checkbox__label-text">{{ item.label }}</span>
            <span v-if="item.hint" class="hybrid-checkbox__hint">{{ item.hint }}</span>
          </label>
        </div>
      </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

interface Option {
  id: string
  value: string | number
  label: string
  hint?: string
  indeterminate?: boolean
}

const props = defineProps({
  modelValue: {
    type: Array as () => (string | number)[],
    default: () => [],
  },
  options: {
    type: Array as () => Option[],
    required: true,
  },
  legend: String,
  hint: String,
  error: String,
})

const emit = defineEmits(['update:modelValue'])

const toggle = (value: string | number) => {
  const newValue = [...props.modelValue]
  const index = newValue.indexOf(value)
  if (index > -1) {
    newValue.splice(index, 1)
  } else {
    newValue.push(value)
  }
  emit('update:modelValue', newValue)
}
</script>

<style scoped>

.hybrid-checklist {
  font-family: var(--font-sans);
  margin-bottom: 2rem;
  width: 100%;
  max-width: 38rem;
}

.hybrid-checklist__fieldset {
  border: none;
  padding: 0;
  margin: 0;
}

.hybrid-checklist__legend {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.5;
  color: var(--color-text-primary);
  margin-bottom: 0.5rem;
}

.hybrid-checklist__helper {
  font-size: 1rem;
  line-height: 1.5;
  color: var(--color-text-secondary);
  margin-bottom: 1rem;
}

.hybrid-checklist__validation {
  margin-bottom: 1rem;
}

.hybrid-checklist__error-text {
  color: var(--color-critical);
  font-size: 1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.hybrid-checklist__error-text::before {
  content: '⚠';
  font-size: 1rem;
}

.hybrid-checklist__group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hybrid-checkbox {
  position: relative;
  display: flex;
  align-items: flex-start;
  min-height: 24px;
  margin-bottom: 0.25rem;
}

.hybrid-checkbox__input {
  position: absolute;
  opacity: 0;
  width: 18px;
  height: 18px;
  margin: 0;
  cursor: pointer;
  z-index: 1;
}

.hybrid-checkbox__input + .hybrid-checkbox__label::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 0;
  width: 18px;
  height: 18px;
  background: var(--color-white);
  border: 1px solid var(--color-border-muted);
  border-radius: 2px;
  box-sizing: border-box;
  transition: all 70ms cubic-bezier(0.2, 0, 0.38, 0.9);
}

.hybrid-checkbox__label {
  position: relative;
  padding-left: 26px;
  font-size: 1rem;
  line-height: 1.5;
  color: var(--color-text-primary);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.hybrid-checkbox__label-text {
  font-weight: 400;
}

.hybrid-checkbox__hint {
  display: block;
  margin-top: 0.125rem;
  font-size: 1rem;
  color: var(--color-text-secondary);
}

.hybrid-checkbox__input:hover + .hybrid-checkbox__label::before {
  border-color: var(--color-text-secondary);
}

.hybrid-checkbox__input:focus + .hybrid-checkbox__label::before {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.hybrid-checkbox__input:checked + .hybrid-checkbox__label::before {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.hybrid-checkbox__input:checked + .hybrid-checkbox__label::after {
  content: '';
  position: absolute;
  top: 6px;
  left: 6px;
  width: 6px;
  height: 10px;
  border: solid var(--color-white);
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.hybrid-checkbox__input:indeterminate + .hybrid-checkbox__label::before {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.hybrid-checkbox__input:indeterminate + .hybrid-checkbox__label::after {
  content: '';
  position: absolute;
  top: 9px;
  left: 4px;
  width: 10px;
  height: 2px;
  background: var(--color-white);
  border: none;
  transform: none;
}

.hybrid-checklist--error .hybrid-checkbox__input + .hybrid-checkbox__label::before {
  border-color: var(--color-critical);
}

.hybrid-checkbox__input:disabled + .hybrid-checkbox__label {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
