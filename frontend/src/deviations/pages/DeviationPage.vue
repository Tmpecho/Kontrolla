<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import {
  addDeviationTimelineNote,
  assignDeviation,
  getDeviation,
  listEstablishmentDeviations,
  listOrganizationMembers,
  mapDeviationResponseToListItem,
  type OrganizationMemberResponse,
  toMemberNameLookup,
  toMemberOptions,
  updateDeviationDetails,
  updateDeviationStatus,
} from '@/deviations/api/deviations.api'
import DeviationDetailPanel from '@/deviations/components/DeviationDetailPanel.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import { ApiError } from '@/shared/api/http'
import AppOverlay from '@/shared/components/overlay/AppOverlay.vue'
import { appEnv } from '@/shared/config/env'
import type {
  DeviationListItem,
  DeviationMemberOption,
  DeviationSaveInput,
  DeviationServiceArea,
} from '@/deviations/model/deviation.types'
import {
  formatDeviationSeverity as formatSeverity,
  formatDeviationStatus as formatStatus,
  toDeviationCategoryValue,
} from '@/deviations/model/deviation.types'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const searchQuery = ref('')
const activeFilter = ref<'ALL' | 'OPEN' | 'RECENT'>('ALL')
const deviations = ref<DeviationListItem[]>([])
const memberOptionsByEstablishment = ref<Record<string, DeviationMemberOption[]>>({})
const memberNamesById = ref<Record<string, string>>({})
const selectedDeviationDetails = ref<DeviationListItem | null>(null)
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const isSaving = ref(false)
const saveErrorMessage = ref<string | null>(null)
const isMobileViewport = ref(false)
const mobileBreakpoint = 960

const filterOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'RECENT', label: 'Recent' },
] as const

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(() => {
  if (authStore.appContext?.organizationId) {
    return authStore.appContext.establishmentId ?? null
  }

  return appEnv.defaultEstablishmentId ?? null
})
const availableEstablishmentIds = computed(() => {
  if (establishmentId.value) {
    return [establishmentId.value]
  }

  return (authStore.establishments ?? []).map((establishment) => establishment.id)
})

const hasDeviationContext = computed(
  () => Boolean(organizationId.value && availableEstablishmentIds.value.length > 0),
)

const missingContextMessage = computed(() => {
  if (hasDeviationContext.value) {
    return null
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose an establishment to load deviations.'
  }

  if (!appEnv.isDevelopment) {
    return 'Deviations cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID or sign in with an organization context to load deviations.'
})

const currentServiceArea = computed<DeviationServiceArea>(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    return 'IK_ALKOHOL'
  }

  return 'IK_MAT'
})

const pageSubtitle = computed(() => {
  if (currentServiceArea.value === 'IK_ALKOHOL') {
    return 'Track, manage and resolve alcohol control deviations, incidents, and follow-up actions.'
  }

  return 'Track, manage and resolve food safety deviations, hygiene issues, and corrective follow-up.'
})

const serviceDeviations = computed(() => {
  return deviations.value
    .filter((deviation) => deviation.serviceArea === currentServiceArea.value)
    .sort(
      (left, right) => new Date(right.reportedAt).getTime() - new Date(left.reportedAt).getTime(),
    )
})

const selectedDeviationId = computed(() => {
  return typeof route.query.deviationId === 'string' ? route.query.deviationId : null
})

const selectedDeviationSummary = computed(() => {
  if (!selectedDeviationId.value) {
    return null
  }

  return serviceDeviations.value.find((deviation) => deviation.id === selectedDeviationId.value) ?? null
})

const selectedDeviation = computed(() => {
  if (!selectedDeviationId.value) {
    return null
  }

  if (selectedDeviationDetails.value?.id === selectedDeviationId.value) {
    return selectedDeviationDetails.value
  }

  return selectedDeviationSummary.value
})

const selectedMemberOptions = computed(() => {
  const resolvedEstablishmentId = selectedDeviation.value?.establishmentId ?? establishmentId.value

  if (!resolvedEstablishmentId) {
    return []
  }

  return memberOptionsByEstablishment.value[resolvedEstablishmentId] ?? []
})

const filteredDeviations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  const items = serviceDeviations.value.filter((deviation) => {
    if (!query) {
      return true
    }

    return [
      deviation.title,
      deviation.category,
      deviation.description,
      deviation.assignedTo.join(' '),
    ]
      .join(' ')
      .toLowerCase()
      .includes(query)
  })

  if (activeFilter.value === 'OPEN') {
    return items.filter((deviation) => deviation.status !== 'RESOLVED')
  }

  if (activeFilter.value === 'RECENT') {
    return items.slice(0, 5)
  }

  return items
})

const emptyStateMessage = computed(() => {
  if (searchQuery.value.trim()) {
    return 'No deviations matched your search.'
  }

  if (activeFilter.value === 'OPEN') {
    return 'No open deviations found.'
  }

  return 'No deviations registered yet.'
})

function goToDeviationForm() {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    router.push({ name: 'ik-alkohol-deviation-form' })
    return
  }

  router.push({ name: 'ik-mat-deviation-form' })
}

function replaceDeviation(updatedDeviation: DeviationListItem) {
  deviations.value = deviations.value.map((deviation) =>
    deviation.id === updatedDeviation.id ? updatedDeviation : deviation,
  )

  if (selectedDeviationId.value === updatedDeviation.id) {
    selectedDeviationDetails.value = updatedDeviation
  }
}

async function loadSelectedDeviation() {
  const resolvedOrganizationId = organizationId.value
  const deviationId = selectedDeviationId.value
  const resolvedEstablishmentId =
    selectedDeviationDetails.value?.establishmentId ??
    selectedDeviationSummary.value?.establishmentId ??
    establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !deviationId) {
    selectedDeviationDetails.value = null
    return
  }

  try {
    const response = await getDeviation({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      deviationId,
    })

    if (selectedDeviationId.value !== deviationId) {
      return
    }

    selectedDeviationDetails.value = mapDeviationResponseToListItem(response, memberNamesById.value)
  } catch (error) {
    if (selectedDeviationId.value !== deviationId) {
      return
    }

    selectedDeviationDetails.value = null

    if (error instanceof ApiError && error.status === 404) {
      await clearSelectedDeviation()
      return
    }

    saveErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load deviation details.'
  }
}

async function loadDeviations() {
  const resolvedOrganizationId = organizationId.value

  if (!resolvedOrganizationId || availableEstablishmentIds.value.length === 0) {
    deviations.value = []
    memberOptionsByEstablishment.value = {}
    selectedDeviationDetails.value = null
    errorMessage.value = null
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const [deviationPages, memberPages] = await Promise.all([
      Promise.all(
        availableEstablishmentIds.value.map((nextEstablishmentId) =>
          listEstablishmentDeviations({
            organizationId: resolvedOrganizationId,
            establishmentId: nextEstablishmentId,
            size: 200,
          }),
        ),
      ),
      Promise.all(
        availableEstablishmentIds.value.map((nextEstablishmentId) =>
          listOrganizationMembers({
            organizationId: resolvedOrganizationId,
            establishmentId: nextEstablishmentId,
            includeInactive: true,
            size: 200,
          })
            .then((page) => ({
              establishmentId: nextEstablishmentId,
              page,
            }))
            .catch(() => ({
              establishmentId: nextEstablishmentId,
              page: null,
            })),
        ),
      ),
    ])

    const nextMemberOptionsByEstablishment: Record<string, DeviationMemberOption[]> = {}
    const mergedMembers = new Map<string, OrganizationMemberResponse>()
    for (const { establishmentId: nextEstablishmentId, page } of memberPages) {
      if (!page) {
        nextMemberOptionsByEstablishment[nextEstablishmentId] = []
        continue
      }

      nextMemberOptionsByEstablishment[nextEstablishmentId] = toMemberOptions(page.items)

      for (const member of page.items) {
        mergedMembers.set(member.userId, member)
      }
    }

    memberOptionsByEstablishment.value = nextMemberOptionsByEstablishment
    memberNamesById.value = toMemberNameLookup([...mergedMembers.values()])

    deviations.value = deviationPages.flatMap((page) => page.items).map((deviation) =>
      mapDeviationResponseToListItem(deviation, memberNamesById.value),
    )

    if (selectedDeviationId.value) {
      await loadSelectedDeviation()
    }
  } catch (error) {
    deviations.value = []
    memberOptionsByEstablishment.value = {}
    memberNamesById.value = {}
    selectedDeviationDetails.value = null
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load deviations.'
  } finally {
    isLoading.value = false
  }
}

function getQueryWithoutSelection() {
  const remainingQuery = { ...route.query }
  delete remainingQuery.deviationId
  return remainingQuery
}

async function selectDeviation(deviationId: string) {
  await router.replace({
    query: {
      ...route.query,
      deviationId,
    },
  })
}

async function clearSelectedDeviation() {
  await router.replace({
    query: getQueryWithoutSelection(),
  })
}

async function handleDeviationSave(nextValues: DeviationSaveInput) {
  const resolvedOrganizationId = organizationId.value
  const currentDeviation = selectedDeviation.value
  const resolvedEstablishmentId = currentDeviation?.establishmentId ?? establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !currentDeviation) {
    return
  }

  isSaving.value = true
  saveErrorMessage.value = null

  try {
    let updatedDeviation = currentDeviation

    const detailsChanged =
      nextValues.title !== currentDeviation.title ||
      nextValues.description !== currentDeviation.description ||
      nextValues.category !== currentDeviation.category ||
      nextValues.severity !== currentDeviation.severity

    if (detailsChanged) {
      const response = await updateDeviationDetails({
        organizationId: resolvedOrganizationId,
        establishmentId: resolvedEstablishmentId,
        deviationId: currentDeviation.id,
        title: nextValues.title,
        description: nextValues.description,
        category: toDeviationCategoryValue(nextValues.category),
        severity: nextValues.severity,
      })
      updatedDeviation = mapDeviationResponseToListItem(response, memberNamesById.value)
      replaceDeviation(updatedDeviation)
    }

    if (nextValues.status !== updatedDeviation.status) {
      const response = await updateDeviationStatus({
        organizationId: resolvedOrganizationId,
        establishmentId: resolvedEstablishmentId,
        deviationId: currentDeviation.id,
        status: nextValues.status,
      })
      updatedDeviation = mapDeviationResponseToListItem(response, memberNamesById.value)
      replaceDeviation(updatedDeviation)
    }

    if (nextValues.assignedToUserId !== updatedDeviation.assignedToUserId) {
      if (!nextValues.assignedToUserId) {
        saveErrorMessage.value = 'Removing an assignee is not supported yet.'
        return
      }

      const response = await assignDeviation({
        organizationId: resolvedOrganizationId,
        establishmentId: resolvedEstablishmentId,
        deviationId: currentDeviation.id,
        assignedUserId: nextValues.assignedToUserId,
      })
      updatedDeviation = mapDeviationResponseToListItem(response, memberNamesById.value)
      replaceDeviation(updatedDeviation)
    }
  } catch (error) {
    saveErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to save deviation.'
  } finally {
    isSaving.value = false
  }
}

async function handleTimelineNoteAdd(note: string) {
  const resolvedOrganizationId = organizationId.value
  const currentDeviation = selectedDeviation.value
  const resolvedEstablishmentId = currentDeviation?.establishmentId ?? establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId || !currentDeviation) {
    return
  }

  isSaving.value = true
  saveErrorMessage.value = null

  try {
    const response = await addDeviationTimelineNote({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      deviationId: currentDeviation.id,
      note,
    })

    replaceDeviation(mapDeviationResponseToListItem(response, memberNamesById.value))
  } catch (error) {
    saveErrorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to add follow-up note.'
  } finally {
    isSaving.value = false
  }
}

function onDeviationRowKeydown(event: KeyboardEvent, deviationId: string) {
  if (event.key !== 'Enter' && event.key !== ' ') {
    return
  }

  event.preventDefault()
  void selectDeviation(deviationId)
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function updateViewportMode() {
  isMobileViewport.value = window.innerWidth <= mobileBreakpoint
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && selectedDeviation.value && !isMobileViewport.value) {
    void clearSelectedDeviation()
  }
}

watch([organizationId, establishmentId, availableEstablishmentIds], () => {
  void loadDeviations()
}, { immediate: true })

watch([currentServiceArea, selectedDeviationId], async () => {
  if (selectedDeviationId.value && !selectedDeviationSummary.value) {
    await clearSelectedDeviation()
  }
})

watch(selectedDeviationId, () => {
  saveErrorMessage.value = null

  if (!selectedDeviationId.value) {
    selectedDeviationDetails.value = null
    return
  }

  void loadSelectedDeviation()
})

onMounted(() => {
  updateViewportMode()
  document.addEventListener('keydown', handleEscape)
  window.addEventListener('resize', updateViewportMode)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleEscape)
  window.removeEventListener('resize', updateViewportMode)
})
</script>

<template>
  <div class="deviation-page app-page">
    <div
      class="deviation-content"
      :class="{ 'deviation-content--with-detail': Boolean(selectedDeviation) }"
    >
      <div class="deviation-primary">
        <header class="page-header app-page-header">
          <div class="page-header-copy app-page-header-copy">
            <h1 class="app-page-title">Deviations</h1>
            <p class="page-subtitle app-page-subtitle">{{ pageSubtitle }}</p>
          </div>

          <BaseButton class="add-button" type="button" @click="goToDeviationForm">
            <span class="add-button-content">
              <svg aria-hidden="true" class="add-button-icon" viewBox="0 0 20 20">
                <path
                  d="M10 4.5v11"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2.0"
                />
                <path
                  d="M4.5 10h11"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2.0"
                />
              </svg>
              <span>Add deviation</span>
            </span>
          </BaseButton>
        </header>

        <section aria-label="Deviation overview" class="list-panel app-panel">
          <div class="list-toolbar app-toolbar">
            <div class="search-field app-search-field">
              <label class="search-label app-search-label" for="deviation-search">Search</label>
              <input
                id="deviation-search"
                v-model="searchQuery"
                class="search-input app-search-input"
                placeholder="Search deviations"
                type="search"
              />
            </div>

            <div aria-label="Deviation filters" class="filter-group app-filter-group">
              <button
                v-for="filterOption in filterOptions"
                :key="filterOption.value"
                :data-active="activeFilter === filterOption.value"
                class="filter-chip app-filter-chip"
                type="button"
                @click="activeFilter = filterOption.value"
              >
                {{ filterOption.label }}
              </button>
            </div>
          </div>

          <div v-if="missingContextMessage" class="empty-state">
            <p>{{ missingContextMessage }}</p>
          </div>

          <div v-else-if="isLoading" class="empty-state">
            <p>Loading deviations...</p>
          </div>

          <div v-else-if="errorMessage" class="empty-state">
            <p>{{ errorMessage }}</p>
          </div>

          <ul v-else-if="filteredDeviations.length > 0" class="deviation-list">
            <li
              v-for="deviation in filteredDeviations"
              :key="deviation.id"
              class="deviation-list-item"
            >
              <article
                class="deviation-row"
                :data-selected="selectedDeviationId === deviation.id"
                role="region"
                tabindex="0"
                @click="selectDeviation(deviation.id)"
                @keydown="onDeviationRowKeydown($event, deviation.id)"
              >
                <div class="deviation-row-header">
                  <div>
                    <h2>{{ deviation.title }}</h2>
                    <p class="deviation-row-hint">
                      Open to view assignees and corrective follow-up.
                    </p>
                  </div>
                  <span aria-hidden="true" class="deviation-row-chevron">›</span>
                </div>

                <dl class="deviation-metadata">
                  <div class="metadata-item">
                    <dt>Reported</dt>
                    <dd>{{ formatDateTime(deviation.reportedAt) }}</dd>
                  </div>
                  <div class="metadata-item">
                    <dt>Category</dt>
                    <dd>{{ deviation.category }}</dd>
                  </div>
                  <div class="metadata-item">
                    <dt>Severity</dt>
                    <dd>
                      <span :data-tone="formatSeverity(deviation.severity)" class="deviation-tag">
                        {{ formatSeverity(deviation.severity) }}
                      </span>
                    </dd>
                  </div>
                  <div class="metadata-item">
                    <dt>Status</dt>
                    <dd>
                      <span :data-tone="formatStatus(deviation.status)" class="deviation-tag">
                        {{ formatStatus(deviation.status) }}
                      </span>
                    </dd>
                  </div>
                </dl>
              </article>
            </li>
          </ul>

          <div v-else class="empty-state">
            <p>{{ emptyStateMessage }}</p>
          </div>
        </section>
      </div>

      <aside v-if="selectedDeviation && !isMobileViewport" class="detail-panel-shell">
        <DeviationDetailPanel
          :deviation="selectedDeviation"
          :is-saving="isSaving"
          :member-options="selectedMemberOptions"
          :save-error-message="saveErrorMessage"
          :show-close-button="true"
          @add-note="handleTimelineNoteAdd"
          @close="clearSelectedDeviation"
          @save="handleDeviationSave"
        />
      </aside>
    </div>

    <AppOverlay
      :open="Boolean(selectedDeviation) && isMobileViewport"
      aria-label="Selected deviation details"
      variant="drawer-right"
      @close="clearSelectedDeviation"
    >
      <div v-if="selectedDeviation" class="detail-drawer-shell">
        <DeviationDetailPanel
          :deviation="selectedDeviation"
          :is-saving="isSaving"
          :member-options="selectedMemberOptions"
          :save-error-message="saveErrorMessage"
          :show-close-button="true"
          @add-note="handleTimelineNoteAdd"
          @close="clearSelectedDeviation"
          @save="handleDeviationSave"
        />
      </div>
    </AppOverlay>
  </div>
</template>

<style scoped>
.deviation-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.deviation-content {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.deviation-content--with-detail {
  display: grid;
  flex: 1;
  min-height: 0;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.9fr);
  gap: 24px;
  align-items: stretch;
  overflow: hidden;
}

.deviation-primary {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: var(--page-section-gap);
}

.page-subtitle,
.deviation-row h2,
.deviation-row-hint,
.metadata-item dt,
.metadata-item dd,
.empty-state p {
  margin: 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  max-width: 72ch;
}

.add-button {
  width: auto;
  min-width: 140px;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.add-button-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.add-button-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  fill: none;
}

.list-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.deviation-list {
  margin: 0;
  list-style: none;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: 0;
}

.deviation-list-item + .deviation-list-item {
  border-top: 1px solid #e2e8f0;
}

.deviation-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 20px;
  background-color: var(--color-container);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.deviation-row:hover {
  background-color: #f8fafc;
}

.deviation-row:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.deviation-row[data-selected='true'] {
  background-color: var(--color-surface);
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.deviation-row-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.deviation-row h2 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.deviation-row-hint {
  margin-top: 4px;
  color: var(--color-text-secondary);
}

.deviation-row-chevron {
  color: var(--color-text-secondary);
  font-size: 1.125rem;
  line-height: 1;
}

.deviation-metadata {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px 16px;
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metadata-item dt {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.metadata-item dd {
  color: var(--color-text-primary);
}

.deviation-tag {
  display: inline-flex;
  align-self: flex-start;
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.deviation-tag[data-tone='low'],
.deviation-tag[data-tone='resolved'] {
  color: var(--color-success);
}

.deviation-tag[data-tone='medium'],
.deviation-tag[data-tone='in progress'] {
  color: var(--color-warning);
}

.deviation-tag[data-tone='high'],
.deviation-tag[data-tone='open'] {
  color: var(--color-primary);
}

.deviation-tag[data-tone='critical'] {
  color: var(--color-critical);
}

.empty-state {
  padding: 32px 20px;
}

.empty-state p {
  color: var(--color-text-secondary);
}

.detail-panel-shell {
  align-self: stretch;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.detail-panel-shell :deep(.detail-panel) {
  height: 100%;
  overflow-y: auto;
}

.detail-drawer-shell {
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  overflow-y: auto;
}

.detail-drawer-shell :deep(.detail-panel) {
  min-height: 100%;
}

@media (max-width: 720px) {
  .add-button {
    width: 100%;
  }
}

@media (max-width: 960px) {
  .deviation-content--with-detail {
    grid-template-columns: 1fr;
  }

  .detail-panel-shell {
    display: none;
  }

  .deviation-page,
  .deviation-content,
  .deviation-primary,
  .list-panel,
  .deviation-list {
    height: auto;
    min-height: initial;
    overflow: visible;
  }
}
</style>
