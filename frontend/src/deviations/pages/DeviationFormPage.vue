<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'

import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

const route = useRoute()
const form = reactive({ title: '', category: '', description: '', date: '' })

function formatDateForInput(value: string | null): string {
  if (!value) {
    return ''
  }

  const parsedDate = new Date(value)

  if (Number.isNaN(parsedDate.getTime())) {
    return ''
  }

  const year = parsedDate.getFullYear()
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const day = String(parsedDate.getDate()).padStart(2, '0')
  const hours = String(parsedDate.getHours()).padStart(2, '0')
  const minutes = String(parsedDate.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function syncFormFromQuery(): void {
  form.title = typeof route.query.title === 'string' ? route.query.title : ''
  form.category = typeof route.query.category === 'string' ? route.query.category : ''
  form.description = typeof route.query.description === 'string' ? route.query.description : ''
  form.date = formatDateForInput(typeof route.query.date === 'string' ? route.query.date : null)
}

async function onSubmit() {

}

watch(
  () => route.query,
  () => {
    syncFormFromQuery()
  },
  { immediate: true },
)
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
              <option value="temperature">Temperature</option>
              <option value="hygiene">Hygiene</option>
              <option value="storage">Storage</option>
              <option value="equipment">Equipment</option>
              <option value="other">Other</option>
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
