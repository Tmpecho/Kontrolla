<script setup lang="ts">
import { reactive } from 'vue'

import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

const form = reactive({ title: '', description: '', date: '' })

async function onSubmit() {

}
</script>

<template>
<div class="page-container">
    <h2>Diviation Form</h2>

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
            type="date"
            v-model="form.date"
            />
        </div>
        <div class="btn-wrapper">
            <BaseButton>Submit</BaseButton>
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
</style>
