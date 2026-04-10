<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { useProtectedWorkspaceContext } from '@/auth/model/workspace-context'
import { createTemperatureUnit } from '@/ik-mat/api/temperature.api'
import type { TemperatureUnitType } from '@/ik-mat/model/temperature.types'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'

const authStore = useAuthStore()
const workspaceContext = useProtectedWorkspaceContext()
const router = useRouter()

const form = reactive({
  name: '',
  location: '',
  type: 'FRIDGE' as TemperatureUnitType,
  dueByTime: '',
  minimumTemperature: '',
  maximumTemperature: '',
})

const validationMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)
const isSubmitting = ref(false)

const organizationId = workspaceContext.organizationId
const establishmentId = workspaceContext.establishmentId

const canManageTemperatureUnits = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN'
  )
})

const missingContextMessage = computed(() => {
  if (workspaceContext.isStartupPending.value) {
    return null
  }

  if (workspaceContext.hasEstablishmentContext.value) {
    return null
  }

  if (workspaceContext.requiresEstablishmentSelection.value) {
    return 'Choose an establishment before creating a temperature unit.'
  }

  return 'Temperature units cannot be created until organization and establishment context is available.'
})

const blockedMessage = computed(() => {
  if (missingContextMessage.value) {
    return missingContextMessage.value
  }

  if (canManageTemperatureUnits.value) {
    return null
  }

  return 'Only organization admins can create new temperature units.'
})

function validateForm(): boolean {
  if (
    !form.name.trim() ||
    !form.location.trim() ||
    !form.dueByTime ||
    !form.minimumTemperature.trim() ||
    !form.maximumTemperature.trim()
  ) {
    validationMessage.value = 'Complete all fields before creating a temperature unit.'
    return false
  }

  const minimumTemperature = Number.parseFloat(form.minimumTemperature.replace(',', '.'))
  const maximumTemperature = Number.parseFloat(form.maximumTemperature.replace(',', '.'))

  if (Number.isNaN(minimumTemperature) || Number.isNaN(maximumTemperature)) {
    validationMessage.value = 'Enter valid numeric temperature thresholds.'
    return false
  }

  if (maximumTemperature < minimumTemperature) {
    validationMessage.value = 'Maximum temperature must be equal to or above the minimum.'
    return false
  }

  validationMessage.value = null
  return true
}

async function submitForm() {
  if (isSubmitting.value || !canManageTemperatureUnits.value) {
    return
  }

  if (!validateForm()) {
    return
  }

  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    await createTemperatureUnit({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      name: form.name.trim(),
      location: form.location.trim(),
      type: form.type,
      dueByTime: `${form.dueByTime}:00`,
      minimumTemperature: Number.parseFloat(form.minimumTemperature.replace(',', '.')),
      maximumTemperature: Number.parseFloat(form.maximumTemperature.replace(',', '.')),
    })

    await router.push({ name: 'ik-mat-temperature' })
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to create temperature unit.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="create-page">
    <header class="page-header">
      <h1>Add new temperature unit</h1>
      <p class="page-subtitle">
        Register a fridge or freezer, define its daily logging deadline, and set the accepted
        temperature range for the team.
      </p>
    </header>

    <section v-if="blockedMessage" class="placeholder-panel">
      <h2>Creation unavailable</h2>
      <p>{{ blockedMessage }}</p>

      <RouterLink :to="{ name: 'ik-mat-temperature' }" class="back-link">
        Back to temperature log
      </RouterLink>
    </section>

    <form v-else class="create-form" @submit.prevent="submitForm">
      <div class="form-grid">
        <label class="field">
          <span class="field-label">Unit name</span>
          <input
            v-model="form.name"
            class="field-input"
            type="text"
            maxlength="255"
          />
        </label>

        <label class="field">
          <span class="field-label">Location</span>
          <input
            v-model="form.location"
            class="field-input"
            type="text"
            maxlength="255"
          />
        </label>

        <label class="field">
          <span class="field-label">Unit type</span>
          <select v-model="form.type" class="field-input">
            <option value="FRIDGE">Fridge</option>
            <option value="FREEZER">Freezer</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">Daily due time</span>
          <input
            v-model="form.dueByTime"
            class="field-input"
            type="time"
          />
        </label>

        <label class="field">
          <span class="field-label">Minimum temperature</span>
          <input
            v-model="form.minimumTemperature"
            class="field-input"
            inputmode="decimal"
            type="text"
            placeholder="e.g. 2"
          />
        </label>

        <label class="field">
          <span class="field-label">Maximum temperature</span>
          <input
            v-model="form.maximumTemperature"
            class="field-input"
            inputmode="decimal"
            type="text"
            placeholder="e.g. 4"
          />
        </label>
      </div>

      <p v-if="validationMessage" class="feedback-message feedback-message-error">
        {{ validationMessage }}
      </p>
      <p v-if="errorMessage" class="feedback-message feedback-message-error">
        {{ errorMessage }}
      </p>

      <div class="form-actions">
        <RouterLink :to="{ name: 'ik-mat-temperature' }" class="secondary-link">
          Back to temperature log
        </RouterLink>

        <BaseButton class="submit-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Saving...' : 'Create unit' }}
        </BaseButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.create-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.page-header h1,
.page-subtitle,
.placeholder-panel h2,
.placeholder-panel p,
.feedback-message {
  margin: 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  max-width: 64ch;
}

.placeholder-panel {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.field-input {
  width: 100%;
  padding: 0.875rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.field-input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-color: transparent;
}

.feedback-message-error {
  color: var(--color-critical);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.back-link {
  align-self: flex-start;
  margin-top: 8px;
  text-decoration: none;
  padding: 0.875rem 1rem;
  border-radius: 4px;
  background-color: var(--color-primary);
  color: var(--color-white);
  font-size: 0.875rem;
  font-weight: 600;
}

.back-link:hover {
  background-color: color-mix(in srgb, var(--color-primary) 88%, black);
}

.secondary-link {
  text-decoration: none;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.secondary-link:hover {
  color: var(--color-text-primary);
}

.submit-button {
  width: auto;
  min-width: 0;
}

.submit-button :deep(.button) {
  width: auto;
  justify-content: center;
  padding: 0.875rem 1rem;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .submit-button {
    width: 100%;
  }

  .submit-button :deep(.button) {
    width: 100%;
  }
}
</style>
