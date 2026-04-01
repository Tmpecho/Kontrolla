<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/shared/components/BaseButton.vue'

const route = useRoute()
const router = useRouter()

function goToDeviationPage() {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    router.push({ name: 'ik-alkohol-deviation-form' })
    return
  }

  router.push({ name: 'ik-mat-deviation-form' })
}
</script>

<template>
    <h1>Deviations</h1>
    <p>overview of deviations ... </p>
    <BaseButton type="button" class="add-btn" @click="goToDeviationPage">
      Add Deviation
    </BaseButton>
</template>

<style scoped>
</style>
