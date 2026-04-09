<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'

const props = withDefaults(
  defineProps<{
    variant?: 'compact' | 'panel'
  }>(),
  {
    variant: 'compact',
  },
)

const authStore = useAuthStore()

const isVisible = computed(() => {
  return authStore.isPlatformAdmin && authStore.organizations.length > 1
})

const selectedOrganizationId = computed(() => authStore.appContext?.organizationId ?? '')
const selectId = computed(() =>
  props.variant === 'panel' ? 'organization-switcher-panel' : 'organization-switcher-compact',
)

async function onSelectionChange(event: Event) {
  const select = event.target as HTMLSelectElement
  const nextValue = select.value.trim()
  await authStore.updateSelectedOrganization(nextValue || null)
}
</script>

<template>
  <div
    v-if="isVisible"
    class="switcher"
    :class="{
      'switcher-compact': props.variant === 'compact',
      'switcher-panel': props.variant === 'panel',
    }"
  >
    <label class="switcher-label" :for="selectId">Organization</label>
    <select
      :id="selectId"
      class="switcher-select"
      aria-label="Organization"
      :value="selectedOrganizationId"
      :disabled="authStore.isLoadingOrganizations"
      @change="onSelectionChange"
    >
      <option
        v-for="organization in authStore.organizations"
        :key="organization.id"
        :value="organization.id"
      >
        {{ organization.name }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.switcher {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.switcher-compact {
  min-width: 220px;
}

.switcher-panel {
  gap: 8px;
}

.switcher-label {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.switcher-select {
  min-height: 40px;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
}

.switcher-select:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.switcher-panel .switcher-select {
  width: 100%;
}
</style>
