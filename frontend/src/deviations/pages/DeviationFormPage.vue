<script setup lang="ts">
import { reactive } from 'vue'

import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

const form = reactive({ title: '', category: '', description: '', date: '' })

async function onSubmit() {

}
</script>

<template>
<div class="page-container">
    <h2>Deviation Form</h2>

    <form @submit.prevent="onSubmit" class="form-wrapper">
        <div class="input-group">
            <BaseInput
            id="title"
            label="title"
            type="text"
            v-model="form.title"
            />
        </div>
        <div class="input-group">
            <label for="category" class="input-label">category</label>
            <select id="category" v-model="form.category" class="input-field" required>
              <option value="" disabled>Select category</option>
              <option value="category-1">Category 1</option>
              <option value="category-2">Category 2</option>
              <option value="category-3">Category 3</option>
            </select>
        </div>
        <div class="input-group">
            <BaseInput
            id="description"
            label="description"
            type="text-area"
            v-model="form.description"
            />
        </div>
        <div class="input-group">
            <BaseInput
            id="date"
            label="date"
            type="datetime-local"
            v-model="form.date"
            />
        </div>
        <div class="btn-wrapper">
            <BaseButton type="submit">Submit</BaseButton>
        </div>
    </form>

</div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-wrapper {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.5rem;
}

.input-field {
  background-color: var(--color-container);
  border: none;
  border-bottom: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.875rem 0.5rem;
  font-size: 1rem;
  color: var(--color-text-primary);
  width: 100%;
  box-sizing: border-box;
}

</style>
