<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseButton from '@/shared/components/BaseButton.vue'

const props = defineProps<{
  to: string
}>()

const router = useRouter()

function goToDeviationPage() {
  router.push(props.to)
}
</script>

<template>
  <div class="deviations-tile">
    <h2>Deviations</h2>
    <p>Empty</p>

    <BaseButton type="button" class="add-btn" @click="goToDeviationPage">
      Add Deviation
    </BaseButton>
  </div>
</template>

<style scoped>

.add-btn {
  align-self: flex-start;
  width: auto;
}
</style>
