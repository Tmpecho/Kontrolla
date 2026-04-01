<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseButton from '@/shared/components/BaseButton.vue'

const props = defineProps<{
  to: string
}>()

const router = useRouter()

function goToDiviationPage() {
  router.push(props.to)
}
</script>

<template>
  <div class="diviations-tile">
    <h2>Diviations</h2>
    <p>Empty</p>

    <BaseButton type="button" class="add-btn" @click="goToDiviationPage">
      Add Diviation
    </BaseButton>
  </div>
</template>

<style scoped>

.add-btn {
  align-self: flex-start;
  width: auto;
}
</style>
