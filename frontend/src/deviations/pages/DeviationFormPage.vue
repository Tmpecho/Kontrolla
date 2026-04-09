<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
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

const authStore = useAuthStore()
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

watch(
  categoryOptions,
  (nextCategoryOptions) => {
    if (!nextCategoryOptions.includes(form.category as DeviationCategory)) {
      form.category = nextCategoryOptions[0] ?? ''
    }
  },
  { immediate: true },
)

const canSubmit = computed(() => {
  return Boolean(
    form.title.trim() &&
      form.description.trim() &&
      form.category &&
      organizationId.value &&
      establishmentId.value &&
      !isSubmitting.value,
  )
})

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

async function onSubmit() {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!canSubmit.value || !resolvedOrganizationId || !resolvedEstablishmentId || !form.category) {
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
  <div class="page-container">
    <div>
      <h2>Deviation Form</h2>
      <p class="page-subtitle">{{ pageSubtitle }}</p>
    </div>

    <form @submit.prevent="onSubmit" class="form-wrapper">
      <p v-if="missingContextMessage" class="form-message">{{ missingContextMessage }}</p>
      <p v-else-if="errorMessage" class="form-message">{{ errorMessage }}</p>

      <div class="input-group">
        <BaseInput
          id="title"
          label="title"
          type="text"
          v-model="form.title"
        />
      </div>

      <div class="form-row">
        <div class="input-group">
          <label for="category" class="input-label">category</label>
          <select id="category" v-model="form.category" class="input-field" required>
            <option v-for="category in categoryOptions" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
        </div>

        <div class="input-group">
          <label for="severity" class="input-label">severity</label>
          <select id="severity" v-model="form.severity" class="input-field" required>
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
        />
      </div>

      <div class="btn-wrapper">
        <BaseButton :disabled="!canSubmit" type="submit">
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
  gap: 1.5rem;
}

.page-subtitle {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}

.form-wrapper {
  display: flex;
  flex-direction: column;
  gap: 30px;
  max-width: 760px;
}

.form-row {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.5rem;
}

.input-field {
  background-color: var(--color-container);
  border: none;
  border-bottom: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.875rem 0.5rem;
  font-size: 1rem;
  color: var(--color-text-primary);
  width: 100%;
  box-sizing: border-box;
}

.input-field:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-bottom-color: transparent;
}

.form-message {
  margin: 0;
  color: var(--color-text-secondary);
}

@media (max-width: 720px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

</style>
