<script setup lang="ts">
import BaseButton from '@/shared/components/BaseButton.vue';
</script>

<template>
  <div class="diviations-tile">
    <h2>Diviations</h2>
    <p>Empty</p>

    <BaseButton type="button" class="add-btn">
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