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
  return Boolean(authStore.appContext?.organizationId) && authStore.establishments.length > 0
})

const selectedEstablishmentId = computed(() => authStore.appContext?.establishmentId ?? '')

const helperCopy = computed(() => {
  if (authStore.isLoadingEstablishments) {
    return 'Loading establishments...'
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose the establishment you are working in.'
  }

  return 'All operational pages use this active establishment.'
})

function onSelectionChange(event: Event) {
  const select = event.target as HTMLSelectElement
  const nextValue = select.value.trim()
  authStore.updateSelectedEstablishment(nextValue || null)
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
    <label class="switcher-label">Establishment</label>
    <select
      class="switcher-select"
      aria-label="Establishment"
      :value="selectedEstablishmentId"
      :disabled="authStore.isLoadingEstablishments"
      @change="onSelectionChange"
    >
      <option
        v-if="authStore.establishments.length > 1"
        value=""
      >
        Select establishment
      </option>
      <option
        v-for="establishment in authStore.establishments"
        :key="establishment.id"
        :value="establishment.id"
      >
        {{ establishment.name }}
      </option>
    </select>
    <p class="switcher-helper">{{ helperCopy }}</p>
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

.switcher-label,
.switcher-helper {
  margin: 0;
}

.switcher-label {
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

.switcher-helper {
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
  line-height: 1.35;
}

.switcher-panel .switcher-select {
  width: 100%;
}
</style>
