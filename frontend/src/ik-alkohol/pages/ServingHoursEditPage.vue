<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import {
  listServingHours,
  updateServingHours,
} from '@/establishments/api/serving-hours.api'
import type {
  ServingHoursDay,
  ServingHoursDayOfWeek,
} from '@/establishments/model/serving-hours.types'
import { ApiError } from '@/shared/api/http'
import BaseButton from '@/shared/components/BaseButton.vue'
import { appEnv } from '@/shared/config/env'

type ServingHoursFormDay = {
  dayOfWeek: ServingHoursDayOfWeek
  closed: boolean
  opensAt: string
  closesAt: string
}

const DAY_LABELS: Record<ServingHoursDayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
}

const authStore = useAuthStore()
const router = useRouter()

const days = reactive<ServingHoursFormDay[]>([])
const isLoading = ref(false)
const isSubmitting = ref(false)
const loadErrorMessage = ref<string | null>(null)
const validationMessage = ref<string | null>(null)
const submitErrorMessage = ref<string | null>(null)

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(
  () => authStore.appContext?.establishmentId ?? appEnv.defaultEstablishmentId ?? null,
)

const canManageServingHours = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN' ||
    authStore.appContext?.organizationRole === 'ORG_MANAGER'
  )
})

const missingContextMessage = computed(() => {
  if (organizationId.value && establishmentId.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Serving hours cannot be edited until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID or sign in with an organization context to edit serving hours.'
})

const blockedMessage = computed(() => {
  if (missingContextMessage.value) {
    return missingContextMessage.value
  }

  if (canManageServingHours.value) {
    return null
  }

  return 'Only organization managers and admins can edit serving hours.'
})

function toFormDay(day: ServingHoursDay): ServingHoursFormDay {
  return {
    dayOfWeek: day.dayOfWeek,
    closed: day.closed,
    opensAt: normalizeTime(day.opensAt),
    closesAt: normalizeTime(day.closesAt),
  }
}

function normalizeTime(value: string | null): string {
  if (!value) {
    return ''
  }

  return value.slice(0, 5)
}

function setDays(nextDays: ServingHoursDay[]) {
  days.splice(0, days.length, ...nextDays.map(toFormDay))
}

function onClosedChange(day: ServingHoursFormDay) {
  if (!day.closed) {
    return
  }

  day.opensAt = ''
  day.closesAt = ''
}

function formatDayLabel(dayOfWeek: ServingHoursDayOfWeek) {
  return DAY_LABELS[dayOfWeek]
}

function validateForm(): boolean {
  for (const day of days) {
    if (day.closed) {
      continue
    }

    if (!day.opensAt || !day.closesAt) {
      validationMessage.value = 'Open days must include both opening and closing times.'
      return false
    }

    if (day.opensAt === day.closesAt) {
      validationMessage.value = 'Opening and closing times must differ.'
      return false
    }
  }

  validationMessage.value = null
  return true
}

function toRequestDay(day: ServingHoursFormDay): ServingHoursDay {
  return {
    dayOfWeek: day.dayOfWeek,
    closed: day.closed,
    opensAt: day.closed ? null : `${day.opensAt}:00`,
    closesAt: day.closed ? null : `${day.closesAt}:00`,
  }
}

async function loadServingHours() {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !canManageServingHours.value) {
    days.splice(0, days.length)
    isLoading.value = false
    loadErrorMessage.value = null
    return
  }

  isLoading.value = true
  loadErrorMessage.value = null

  try {
    const response = await listServingHours({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
    })
    setDays(response)
  } catch (error) {
    days.splice(0, days.length)
    loadErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load serving hours.'
  } finally {
    isLoading.value = false
  }
}

async function submitForm() {
  if (isSubmitting.value || !canManageServingHours.value) {
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
  submitErrorMessage.value = null

  try {
    const response = await updateServingHours({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      days: days.map(toRequestDay),
    })
    setDays(response)
    await router.push({ name: 'ik-alkohol-dashboard' })
  } catch (error) {
    submitErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to save serving hours.'
  } finally {
    isSubmitting.value = false
  }
}

watch([organizationId, establishmentId, canManageServingHours], () => {
  void loadServingHours()
}, { immediate: true })
</script>

<template>
  <div class="edit-page">
    <header class="page-header">
      <h1>Edit serving hours</h1>
      <p class="page-subtitle">
        Set opening and closing times for each weekday for the selected establishment.
      </p>
    </header>

    <section v-if="blockedMessage" class="placeholder-panel">
      <h2>Editing unavailable</h2>
      <p>{{ blockedMessage }}</p>

      <RouterLink :to="{ name: 'ik-alkohol-dashboard' }" class="back-link">
        Back to dashboard
      </RouterLink>
    </section>

    <section v-else-if="isLoading" class="placeholder-panel">
      <h2>Loading</h2>
      <p>Loading serving hours...</p>
    </section>

    <section v-else-if="loadErrorMessage" class="placeholder-panel">
      <h2>Serving hours unavailable</h2>
      <p>{{ loadErrorMessage }}</p>
    </section>

    <form v-else class="edit-form" @submit.prevent="submitForm">
      <div class="hours-grid">
        <div v-for="day in days" :key="day.dayOfWeek" class="hours-row">
          <div class="hours-day">
            <p class="hours-day-label">{{ formatDayLabel(day.dayOfWeek) }}</p>
            <label class="closed-toggle">
              <input
                v-model="day.closed"
                type="checkbox"
                @change="onClosedChange(day)"
              />
              <span>Closed</span>
            </label>
          </div>

          <div class="hours-inputs">
            <label class="field">
              <span class="field-label">Opens</span>
              <input
                v-model="day.opensAt"
                class="field-input"
                type="time"
                :disabled="day.closed"
              />
            </label>

            <label class="field">
              <span class="field-label">Closes</span>
              <input
                v-model="day.closesAt"
                class="field-input"
                type="time"
                :disabled="day.closed"
              />
            </label>
          </div>
        </div>
      </div>

      <p v-if="validationMessage" class="feedback-message feedback-message-error">
        {{ validationMessage }}
      </p>
      <p v-if="submitErrorMessage" class="feedback-message feedback-message-error">
        {{ submitErrorMessage }}
      </p>

      <div class="form-actions">
        <RouterLink :to="{ name: 'ik-alkohol-dashboard' }" class="secondary-link">
          Back to dashboard
        </RouterLink>

        <BaseButton class="submit-button" type="submit">
          {{ isSubmitting ? 'Saving...' : 'Save serving hours' }}
        </BaseButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.edit-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.placeholder-panel,
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-form {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-surface);
}

.page-subtitle,
.placeholder-panel p,
.field-label,
.closed-toggle,
.secondary-link {
  color: var(--color-text-secondary);
}

.hours-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hours-row {
  display: grid;
  grid-template-columns: minmax(140px, 180px) 1fr;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border-muted);
}

.hours-row:last-child {
  border-bottom: none;
}

.hours-day {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hours-day-label {
  margin: 0;
  font-weight: 600;
  color: var(--color-text-primary);
}

.closed-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
}

.hours-inputs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 180px));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-input {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-background);
  color: var(--color-text-primary);
}

.field-input:disabled {
  opacity: 0.55;
}

.feedback-message {
  margin: 0;
  font-size: 0.95rem;
}

.feedback-message-error {
  color: var(--color-danger);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.secondary-link,
.back-link {
  text-decoration: none;
}

.submit-button {
  width: auto;
  min-width: 220px;
}

@media (max-width: 760px) {
  .hours-row {
    grid-template-columns: 1fr;
  }

  .hours-inputs {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .submit-button {
    width: 100%;
  }
}
</style>
