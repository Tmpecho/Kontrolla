<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'

import { useProtectedWorkspaceContext } from '@/auth/model/workspace-context'
import { createDeviation } from '@/deviations/api/deviations.api'
import type { DeviationCategory, DeviationSeverity, DeviationServiceArea } from '@/deviations/model/deviation.types'
import {
  deviationCategoriesByServiceArea,
  formatDeviationSeverity,
  toDeviationCategoryValue,
} from '@/deviations/model/deviation.types'
import { ApiError } from '@/shared/api/http'
import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

const workspaceContext = useProtectedWorkspaceContext()
const route = useRoute()
const router = useRouter()
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)

const severityOptions: DeviationSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const currentServiceArea = computed<DeviationServiceArea>(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    return 'IK_ALKOHOL'
  }

  return 'IK_MAT'
})

const categoryOptions = computed(() => deviationCategoriesByServiceArea[currentServiceArea.value])
const organizationId = workspaceContext.organizationId
const establishmentId = workspaceContext.establishmentId

const pageSubtitle = computed(() => {
  if (currentServiceArea.value === 'IK_ALKOHOL') {
    return 'Register alcohol-control deviations and route them into corrective follow-up.'
  }

  return 'Register food-safety deviations and route them into corrective follow-up.'
})

const missingContextMessage = computed(() => {
  if (workspaceContext.isStartupPending.value) {
    return null
  }

  if (workspaceContext.hasEstablishmentContext.value) {
    return null
  }

  if (workspaceContext.requiresEstablishmentSelection.value) {
    return 'Choose an establishment before creating a deviation.'
  }

  return 'A deviation cannot be created until organization and establishment context is available.'
})

const form = reactive<{
  title: string
  category: DeviationCategory | ''
  severity: DeviationSeverity
  description: string
}>({
  title: '',
  category: '',
  severity: 'MEDIUM',
  description: '',
})
const attemptedSubmit = ref(false)

const validationMessage = computed(() => {
  if (!form.title.trim()) {
    return 'Enter a title for the deviation.'
  }

  if (!form.category) {
    return 'Choose a category.'
  }

  if (!form.description.trim()) {
    return 'Enter a description for the deviation.'
  }

  return null
})

const titleError = computed(() =>
  attemptedSubmit.value && !form.title.trim() ? 'Enter a title for the deviation.' : null,
)
const categoryError = computed(() =>
  attemptedSubmit.value && !form.category ? 'Choose a category.' : null,
)
const descriptionError = computed(() =>
  attemptedSubmit.value && !form.description.trim() ? 'Enter a description for the deviation.' : null,
)

watch(
  categoryOptions,
  (nextCategoryOptions) => {
    if (!nextCategoryOptions.includes(form.category as DeviationCategory)) {
      form.category = nextCategoryOptions[0] ?? ''
    }
  },
  { immediate: true },
)

const isSubmitDisabled = computed(
  () => Boolean(isSubmitting.value || missingContextMessage.value),
)

function normalizeCategoryValue(value: string): string {
  return value.trim().toLowerCase().replace(/[_-]+/g, ' ')
}

function resolveCategoryFromQuery(value: unknown): DeviationCategory | '' {
  if (typeof value !== 'string') {
    return ''
  }

  const normalizedValue = normalizeCategoryValue(value)

  return (
    categoryOptions.value.find(
      (category) =>
        normalizeCategoryValue(category) === normalizedValue ||
        normalizeCategoryValue(toDeviationCategoryValue(category)) === normalizedValue,
    ) ?? ''
  )
}

function syncFormFromQuery(): void {
  form.title = typeof route.query.title === 'string' ? route.query.title : ''
  form.category = resolveCategoryFromQuery(route.query.category) || categoryOptions.value[0] || ''
  form.description = typeof route.query.description === 'string' ? route.query.description : ''
}

function goBack(): void {
  void router.push({
    name: currentServiceArea.value === 'IK_ALKOHOL' ? 'ik-alkohol-deviation' : 'ik-mat-deviation',
  })
}

function clearError(): void {
  errorMessage.value = null
}

async function onSubmit() {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (missingContextMessage.value) {
    errorMessage.value = missingContextMessage.value
    return
  }

  if (validationMessage.value) {
    attemptedSubmit.value = true
    errorMessage.value = null
    return
  }

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !form.category) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const deviation = await createDeviation({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      title: form.title.trim(),
      description: form.description.trim(),
      category: toDeviationCategoryValue(form.category),
      severity: form.severity,
    })

    await router.push({
      name: currentServiceArea.value === 'IK_ALKOHOL' ? 'ik-alkohol-deviation' : 'ik-mat-deviation',
      query: {
        deviationId: deviation.id,
      },
    })
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to create deviation.'
  } finally {
    isSubmitting.value = false
  }
}

watch(
  () => route.query,
  () => {
    syncFormFromQuery()
  },
  { immediate: true },
)
</script>

<template>
  <div class="page-container app-page">
    <div class="page-header app-page-header">
      <button type="button" class="back-button" @click="goBack">
        <ArrowLeft :size="16" aria-hidden="true" />
        <span>Back to deviations</span>
      </button>

      <div class="page-header-copy app-page-header-copy">
        <h1 class="app-page-title">Deviation form</h1>
        <p class="page-subtitle app-page-subtitle">{{ pageSubtitle }}</p>
      </div>
    </div>

    <form @submit.prevent="onSubmit" class="form-wrapper app-panel">
      <p v-if="missingContextMessage" class="form-message">{{ missingContextMessage }}</p>
      <p v-else-if="errorMessage" class="form-message">{{ errorMessage }}</p>

      <div class="input-group">
        <BaseInput
          id="title"
          label="title"
          type="text"
          v-model="form.title"
          :error="titleError"
          @update:model-value="clearError"
        />
      </div>

      <div class="form-row">
        <div class="input-group">
          <label for="category" class="input-label">category</label>
          <select
            id="category"
            v-model="form.category"
            class="input-field"
            :class="{ 'input-field-error': Boolean(categoryError) }"
            :aria-invalid="Boolean(categoryError)"
            @change="clearError"
          >
            <option v-for="category in categoryOptions" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
          <p v-if="categoryError" class="input-error">{{ categoryError }}</p>
        </div>

        <div class="input-group">
          <label for="severity" class="input-label">severity</label>
          <select id="severity" v-model="form.severity" class="input-field" @change="clearError">
            <option v-for="severity in severityOptions" :key="severity" :value="severity">
              {{ formatDeviationSeverity(severity) }}
            </option>
          </select>
        </div>
      </div>

      <div class="input-group">
        <BaseInput
          id="description"
          label="description"
          type="text-area"
          v-model="form.description"
          :error="descriptionError"
          @update:model-value="clearError"
        />
      </div>

      <div class="btn-wrapper">
        <BaseButton :disabled="isSubmitDisabled" type="submit">
          {{ isSubmitting ? 'Submitting...' : 'Submit' }}
        </BaseButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.875rem;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
}

.back-button:hover {
  color: color-mix(in srgb, var(--color-primary) 84%, black);
}

.back-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 4px;
}

.form-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 760px;
  padding: var(--panel-padding-md);
}

.form-row {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

@media (max-width: 720px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.input-label {
  font-size: var(--font-size-label);
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: var(--field-label-letter-spacing);
}

.input-field {
  min-height: var(--field-min-height);
  background-color: var(--field-background);
  border: 1px solid var(--field-border-color);
  border-radius: var(--field-radius);
  padding: var(--field-padding-y) var(--field-padding-x);
  font-size: var(--font-size-body);
  color: var(--color-text-primary);
  width: 100%;
  box-sizing: border-box;
}

.input-field:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--field-focus-ring);
}

.input-field-error {
  border-color: var(--color-critical);
}

.input-field-error:focus {
  border-color: var(--color-critical);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-critical) 18%, transparent);
}

.input-error {
  margin: 0;
  font-size: var(--font-size-body-sm);
  color: var(--color-critical);
}

.form-message {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-body-sm);
  line-height: var(--line-height-body);
}
</style>
