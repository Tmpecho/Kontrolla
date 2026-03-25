<script setup lang="ts">
import { reactive } from 'vue'

const form = reactive({
  email: '',
  password: '',
})

function onSubmit() {
  // API wiring will be added later.
}
</script>

<template>
  <form @submit.prevent="onSubmit">
    <div>
      <label for="email">Email</label>
      <input id="email" v-model="form.email" type="email" autocomplete="email" />
    </div>

    <div>
      <label for="password">Password</label>
      <input
        id="password"
        v-model="form.password"
        type="password"
        autocomplete="current-password"
      />
    </div>

    <div>
      <button type="submit">Log in</button>
    </div>
  </form>
</template>
