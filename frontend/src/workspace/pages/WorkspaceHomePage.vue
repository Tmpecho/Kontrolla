<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import { useProtectedWorkspaceContext } from '@/auth/model/workspace-context'
import { listChecklistRuns } from '@/checklists/api/checklist-runs.api'
import { selectLatestChecklistRuns } from '@/checklists/model/checklist-runs.utils'
import type { ChecklistRun } from '@/checklists/model/checklist.types'
import { listEstablishmentDeviations } from '@/deviations/api/deviations.api'
import type { DeviationListItem, DeviationServiceArea } from '@/deviations/model/deviation.types'
import {
  getDeviationServiceAreaForCategory,
  toDeviationCategoryLabel,
} from '@/deviations/model/deviation.types'
import { listAllEstablishmentDocuments } from '@/documents/api/documents.api'
import type { EstablishmentDocument } from '@/documents/model/document.types'
import { listTemperatureUnits } from '@/ik-mat/api/temperature.api'
import type { TemperatureUnit } from '@/ik-mat/model/temperature.types'
import { ApiError } from '@/shared/api/http'
import {
  buildIKAlkoholServiceSummary,
  buildIKMatServiceSummary,
  buildWorkspaceAttentionItems,
} from '@/workspace/model/workspace-dashboard'

const authStore = useAuthStore()
const protectedWorkspaceContext = useProtectedWorkspaceContext()
const checklistRuns = ref<ChecklistRun[] | null>(null)
const isLoadingChecklistRuns = ref(false)
const checklistErrorMessage = ref<string | null>(null)
const deviationsByService = ref<Record<DeviationServiceArea, DeviationListItem[]>>({
  IK_MAT: [],
  IK_ALKOHOL: [],
})
const isLoadingDeviations = ref(false)
const deviationErrorMessage = ref<string | null>(null)
const documents = ref<EstablishmentDocument[] | null>(null)
const isLoadingDocuments = ref(false)
const documentErrorMessage = ref<string | null>(null)
const temperatureUnits = ref<TemperatureUnit[] | null>(null)
const isLoadingTemperatureUnits = ref(false)
const temperatureErrorMessage = ref<string | null>(null)
let workspaceRequestSequence = 0

const workspaceContext = computed(() => {
  if (!authStore.isSessionReady || protectedWorkspaceContext.isStartupPending.value) {
    return null
  }

  if (
    protectedWorkspaceContext.hasOrganizationContext.value &&
    protectedWorkspaceContext.availableEstablishmentIds.value.length > 0
  ) {
    return {
      organizationId: protectedWorkspaceContext.organizationId.value!,
      establishmentIds: protectedWorkspaceContext.availableEstablishmentIds.value,
    }
  }

  return null
})

const ikMatNote = computed(() => {
  if (!authStore.isSessionReady || protectedWorkspaceContext.isStartupPending.value) {
    return 'Loading workspace context...'
  }

  if (!workspaceContext.value) {
    return 'Workspace overview becomes available when organization context is ready.'
  }

  const messages = [
    checklistErrorMessage.value,
    temperatureErrorMessage.value,
    deviationErrorMessage.value,
  ].filter((message): message is string => Boolean(message))

  if (messages.length > 0) {
    return messages.join(' ')
  }

  if (
    (isLoadingChecklistRuns.value && checklistRuns.value === null) ||
    (isLoadingTemperatureUnits.value && temperatureUnits.value === null) ||
    (isLoadingDeviations.value &&
      deviationsByService.value.IK_MAT.length === 0 &&
      deviationsByService.value.IK_ALKOHOL.length === 0)
  ) {
    return 'Loading IK-mat overview...'
  }

  return null
})

const ikAlkoholNote = computed(() => {
  if (!authStore.isSessionReady || protectedWorkspaceContext.isStartupPending.value) {
    return 'Loading workspace context...'
  }

  if (!workspaceContext.value) {
    return 'Workspace overview becomes available when organization context is ready.'
  }

  const messages = [documentErrorMessage.value, deviationErrorMessage.value].filter(
    (message): message is string => Boolean(message),
  )

  if (messages.length > 0) {
    return messages.join(' ')
  }

  if (
    (isLoadingDocuments.value && documents.value === null) ||
    (isLoadingDeviations.value &&
      deviationsByService.value.IK_MAT.length === 0 &&
      deviationsByService.value.IK_ALKOHOL.length === 0)
  ) {
    return 'Loading IK-alkohol overview...'
  }

  return null
})

const attentionMessage = computed(() => {
  if (!authStore.isSessionReady || protectedWorkspaceContext.isStartupPending.value) {
    return 'Loading workspace context...'
  }

  if (!workspaceContext.value) {
    return 'Workspace overview becomes available when organization context is ready.'
  }

  if (
    (isLoadingChecklistRuns.value && checklistRuns.value === null) ||
    (isLoadingTemperatureUnits.value && temperatureUnits.value === null) ||
    (isLoadingDocuments.value && documents.value === null) ||
    (isLoadingDeviations.value &&
      deviationsByService.value.IK_MAT.length === 0 &&
      deviationsByService.value.IK_ALKOHOL.length === 0)
  ) {
    return 'Loading workspace attention...'
  }

  if (
    checklistRuns.value === null ||
    temperatureUnits.value === null ||
    documents.value === null ||
    checklistErrorMessage.value ||
    temperatureErrorMessage.value ||
    documentErrorMessage.value ||
    deviationErrorMessage.value
  ) {
    return 'Workspace attention is partially unavailable while some overview data could not be loaded.'
  }

  return 'No urgent follow-up right now.'
})

const serviceSummaries = computed(() => [
  buildIKMatServiceSummary({
    checklistRuns: checklistRuns.value,
    checklistNote: ikMatNote.value,
    temperatureUnits: temperatureUnits.value,
    deviations: deviationsByService.value.IK_MAT,
  }),
  buildIKAlkoholServiceSummary({
    documents: documents.value,
    deviations: deviationsByService.value.IK_ALKOHOL,
    currentUserId: authStore.user?.id ?? null,
    note: ikAlkoholNote.value,
  }),
])

const attentionItems = computed(() => {
  return buildWorkspaceAttentionItems({
    checklistRuns: checklistRuns.value ?? [],
    temperatureUnits: temperatureUnits.value,
    deviationsByService: deviationsByService.value,
    documents: documents.value,
  }).slice(0, 5)
})

const quickActions = [
  {
    label: 'Open IK-mat dashboard',
    description: 'Jump straight into kitchen follow-up.',
    to: {
      name: 'ik-mat-dashboard',
    },
  },
  {
    label: 'Log temperatures',
    description: 'Open the fridge and freezer logging page.',
    to: {
      name: 'ik-mat-temperature',
    },
  },
  {
    label: 'Review IK-mat deviations',
    description: 'Check open food-safety issues and follow-up.',
    to: {
      name: 'ik-mat-deviation',
    },
  },
  {
    label: 'Open important documents',
    description: 'Review licences, certificates, and renewals.',
    to: {
      name: 'ik-alkohol-documents',
    },
  },
] as const

function mapDeviationSummary(deviation: {
  id: string
  establishmentId: string
  title: string
  description: string
  status: DeviationListItem['status']
  severity: DeviationListItem['severity']
  category: Parameters<typeof toDeviationCategoryLabel>[0]
  createdAt: string
}): DeviationListItem {
  const category = toDeviationCategoryLabel(deviation.category)

  return {
    id: deviation.id,
    establishmentId: deviation.establishmentId,
    serviceArea: getDeviationServiceAreaForCategory(category),
    title: deviation.title,
    reportedAt: deviation.createdAt,
    category,
    severity: deviation.severity,
    status: deviation.status,
    assignedToUserId: null,
    assignedTo: [],
    description: deviation.description,
    timeline: [],
  }
}

async function listAllChecklistRunsForEstablishment(
  organizationId: string,
  establishmentId: string,
): Promise<ChecklistRun[]> {
  const allRuns: ChecklistRun[] = []
  let page = 0
  let totalPages = 1

  do {
    const response = await listChecklistRuns({
      organizationId,
      establishmentId,
      serviceArea: 'IK_MAT',
      page,
      size: 100,
    })

    allRuns.push(...response.items)
    totalPages = response.totalPages
    page += 1
  } while (page < totalPages)

  return allRuns
}

async function listAllDeviationsForEstablishment(
  organizationId: string,
  establishmentId: string,
): Promise<DeviationListItem[]> {
  const allDeviations: DeviationListItem[] = []
  let page = 0
  let totalPages = 1

  do {
    const response = await listEstablishmentDeviations({
      organizationId,
      establishmentId,
      page,
      size: 100,
    })

    allDeviations.push(...response.items.map(mapDeviationSummary))
    totalPages = response.totalPages
    page += 1
  } while (page < totalPages)

  return allDeviations
}

function hasActiveWorkspaceRequest(requestId: number): boolean {
  return requestId === workspaceRequestSequence
}

function resetWorkspaceOverview(): void {
  checklistRuns.value = null
  isLoadingChecklistRuns.value = false
  checklistErrorMessage.value = null
  deviationsByService.value = {
    IK_MAT: [],
    IK_ALKOHOL: [],
  }
  isLoadingDeviations.value = false
  deviationErrorMessage.value = null
  documents.value = null
  isLoadingDocuments.value = false
  documentErrorMessage.value = null
  temperatureUnits.value = null
  isLoadingTemperatureUnits.value = false
  temperatureErrorMessage.value = null
}

async function loadChecklistRuns(
  organizationId: string,
  establishmentIds: string[],
  requestId: number,
): Promise<void> {
  isLoadingChecklistRuns.value = true
  checklistErrorMessage.value = null

  try {
    const pages = await Promise.all(
      establishmentIds.map((establishmentId) =>
        listAllChecklistRunsForEstablishment(organizationId, establishmentId),
      ),
    )

    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    checklistRuns.value = selectLatestChecklistRuns(pages.flat())
  } catch (error) {
    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    checklistRuns.value = null
    checklistErrorMessage.value =
      error instanceof ApiError ? error.message : 'Checklist overview is temporarily unavailable.'
  } finally {
    if (hasActiveWorkspaceRequest(requestId)) {
      isLoadingChecklistRuns.value = false
    }
  }
}

async function loadDeviations(
  organizationId: string,
  establishmentIds: string[],
  requestId: number,
): Promise<void> {
  isLoadingDeviations.value = true
  deviationErrorMessage.value = null

  try {
    const pages = await Promise.all(
      establishmentIds.map((establishmentId) =>
        listAllDeviationsForEstablishment(organizationId, establishmentId),
      ),
    )

    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    const allDeviations = pages.flat()
    deviationsByService.value = {
      IK_MAT: allDeviations.filter((deviation) => deviation.serviceArea === 'IK_MAT'),
      IK_ALKOHOL: allDeviations.filter((deviation) => deviation.serviceArea === 'IK_ALKOHOL'),
    }
  } catch (error) {
    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    deviationsByService.value = {
      IK_MAT: [],
      IK_ALKOHOL: [],
    }
    deviationErrorMessage.value =
      error instanceof ApiError ? error.message : 'Deviation overview is temporarily unavailable.'
  } finally {
    if (hasActiveWorkspaceRequest(requestId)) {
      isLoadingDeviations.value = false
    }
  }
}

async function loadDocuments(
  organizationId: string,
  establishmentIds: string[],
  requestId: number,
): Promise<void> {
  isLoadingDocuments.value = true
  documentErrorMessage.value = null

  try {
    const pages = await Promise.all(
      establishmentIds.map((establishmentId) =>
        listAllEstablishmentDocuments({
          organizationId,
          establishmentId,
          serviceArea: 'IK_ALKOHOL',
          size: 100,
        }),
      ),
    )

    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    documents.value = pages.flat()
  } catch (error) {
    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    documents.value = null
    documentErrorMessage.value =
      error instanceof ApiError ? error.message : 'Documents overview is temporarily unavailable.'
  } finally {
    if (hasActiveWorkspaceRequest(requestId)) {
      isLoadingDocuments.value = false
    }
  }
}

async function loadTemperatureUnits(
  organizationId: string,
  establishmentIds: string[],
  requestId: number,
): Promise<void> {
  isLoadingTemperatureUnits.value = true
  temperatureErrorMessage.value = null

  try {
    const pages = await Promise.all(
      establishmentIds.map((establishmentId) =>
        listTemperatureUnits({
          organizationId,
          establishmentId,
        }),
      ),
    )

    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    temperatureUnits.value = pages.flat()
  } catch (error) {
    if (!hasActiveWorkspaceRequest(requestId)) {
      return
    }

    temperatureUnits.value = null
    temperatureErrorMessage.value =
      error instanceof ApiError
        ? error.message
        : 'Temperature overview is temporarily unavailable.'
  } finally {
    if (hasActiveWorkspaceRequest(requestId)) {
      isLoadingTemperatureUnits.value = false
    }
  }
}

watch(
  () => workspaceContext.value,
  async (context) => {
    workspaceRequestSequence += 1
    const requestId = workspaceRequestSequence

    if (!context) {
      resetWorkspaceOverview()
      return
    }

    resetWorkspaceOverview()

    await Promise.all([
      loadChecklistRuns(context.organizationId, context.establishmentIds, requestId),
      loadDeviations(context.organizationId, context.establishmentIds, requestId),
      loadDocuments(context.organizationId, context.establishmentIds, requestId),
      loadTemperatureUnits(context.organizationId, context.establishmentIds, requestId),
    ])
  },
  { immediate: true },
)
</script>

<template>
  <div class="workspace-page">
    <section class="workspace-header">
      <h1>Workspace</h1>
      <p>Jump into the right service quickly and keep track of the small set of items that need attention now.</p>
    </section>

    <section class="workspace-section">
      <div class="service-grid">
        <RouterLink
          v-for="summary in serviceSummaries"
          :key="summary.key"
          :to="summary.to"
          :data-service="summary.key"
          class="service-tile"
        >
          <div class="service-tile-header">
            <div>
              <h2>{{ summary.title }}</h2>
              <p class="service-description">{{ summary.description }}</p>
            </div>
            <div class="service-header-action">
              <span class="service-cta">{{ summary.ctaLabel }}</span>
              <svg class="service-chevron" viewBox="0 0 20 20" aria-hidden="true">
                <path d="M7 5.5L12 10L7 14.5" />
              </svg>
            </div>
          </div>

          <ul class="service-metrics">
            <li
              v-for="metric in summary.metrics"
              :key="metric.label"
              :data-tone="metric.tone ?? 'neutral'"
              class="service-metric"
            >
              <span class="service-metric-label">{{ metric.label }}</span>
              <strong class="service-metric-value">{{ metric.value }}</strong>
            </li>
          </ul>

          <p v-if="summary.note" class="service-note">{{ summary.note }}</p>
        </RouterLink>
      </div>
    </section>

    <section class="workspace-section">
      <div class="section-header">
        <div>
          <h2>Needs attention</h2>
          <p class="section-subtitle">Cross-service follow-up that is worth opening first.</p>
        </div>
      </div>

      <div class="attention-surface">
        <ul v-if="attentionItems.length > 0" class="attention-list">
          <li v-for="item in attentionItems" :key="item.id" class="attention-list-item">
            <RouterLink :to="item.to" :data-tone="item.tone" class="attention-link">
              <div class="attention-content">
                <div class="attention-meta">
                  <span class="attention-service">{{ item.serviceLabel }}</span>
                  <p class="attention-reason">{{ item.reason }}</p>
                </div>
                <h3 class="attention-title">{{ item.title }}</h3>
              </div>
              <span class="attention-link-label">Open</span>
            </RouterLink>
          </li>
        </ul>

        <p v-else class="attention-empty-state">{{ attentionMessage }}</p>
      </div>
    </section>

    <section class="workspace-section">
      <div class="section-header">
        <div>
          <h2>Quick actions</h2>
          <p class="section-subtitle">Shortcuts to the pages people use most during a shift.</p>
        </div>
      </div>

      <div class="quick-actions-surface">
        <div class="quick-actions-grid">
          <RouterLink
            v-for="action in quickActions"
            :key="action.label"
            :to="action.to"
            class="quick-action-link"
          >
            <div>
              <strong class="quick-action-title">{{ action.label }}</strong>
              <p class="quick-action-description">{{ action.description }}</p>
            </div>
            <svg class="quick-action-chevron" viewBox="0 0 20 20" aria-hidden="true">
              <path d="M7 5.5L12 10L7 14.5" />
            </svg>
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.workspace-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.workspace-header,
.workspace-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workspace-header h1,
.workspace-header p,
.section-header h2,
.section-subtitle,
.service-description,
.service-note,
.attention-service,
.attention-reason,
.attention-title,
.quick-action-title,
.quick-action-description,
.attention-empty-state {
  margin: 0;
}

.workspace-header p,
.section-subtitle,
.service-description,
.service-note,
.attention-reason,
.quick-action-description,
.attention-empty-state {
  color: var(--color-text-secondary);
}

.workspace-header {
  gap: 8px;
}

.workspace-header h1 {
  font-size: 2rem;
  line-height: 1.05;
}

.workspace-header p {
  max-width: 62ch;
}

.service-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.service-tile {
  display: flex;
  min-height: 210px;
  flex-direction: column;
  gap: 14px;
  padding: 20px 22px;
  border: 1px solid var(--color-border-muted);
  border-radius: 1cqh;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
  color: inherit;
  text-decoration: none;
  overflow: hidden;
  transition:
    border-color 120ms ease,
    transform 120ms ease;
}

.service-tile::before {
  content: '';
  display: block;
  width: calc(100% + 44px);
  margin: -20px -22px 0;
  border-top: 2px solid color-mix(in srgb, var(--color-primary) 72%, white);
}

.service-tile[data-service='ik-alkohol']::before {
  border-top-color: color-mix(in srgb, var(--color-warning) 68%, white);
}

.service-tile:hover {
  border-color: var(--color-primary);
  transform: translateY(-1px);
}

.service-tile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.service-tile-header h2 {
  margin: 0;
  font-size: 1.5rem;
}

.service-description {
  margin-top: 6px;
  max-width: 34ch;
  line-height: 1.45;
}

.service-header-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-primary);
  flex-shrink: 0;
}

.service-cta {
  font-size: 0.875rem;
  font-weight: 600;
  white-space: nowrap;
}

.service-chevron {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.75;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.service-metrics {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--color-border-muted);
  border-bottom: 1px solid var(--color-border-muted);
}

.service-metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  padding: 11px 2px;
}

.service-metric + .service-metric {
  border-top: 1px solid var(--color-border-muted);
}

.service-metric-label {
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
}

.service-metric-value {
  font-size: 0.9375rem;
}

.service-metric[data-tone='primary'] .service-metric-value {
  color: var(--color-primary);
}

.service-metric[data-tone='warning'] .service-metric-value {
  color: #b45309;
}

.service-metric[data-tone='critical'] .service-metric-value {
  color: #b91c1c;
}

.service-note {
  margin-top: auto;
  padding-top: 2px;
  line-height: 1.4;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.attention-surface {
  border: 1px solid var(--color-border-muted);
  border-radius: 1cqh;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}

.attention-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.attention-list-item + .attention-list-item {
  border-top: 1px solid var(--color-border-muted);
}

.attention-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  color: inherit;
  text-decoration: none;
  transition: background-color 120ms ease;
}

.attention-link:hover {
  background-color: var(--color-surface);
}

.attention-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.attention-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attention-service {
  color: var(--color-text-primary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.attention-reason {
  font-size: 0.8125rem;
  line-height: 1.35;
}

.attention-title {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.3;
}

.attention-link-label {
  color: var(--color-primary);
  font-size: 0.875rem;
  font-weight: 600;
  white-space: nowrap;
}

.attention-link[data-tone='warning'] .attention-service {
  color: #b45309;
}

.attention-link[data-tone='critical'] .attention-service {
  color: #b91c1c;
}

.attention-empty-state {
  padding: 18px;
}

.quick-actions-surface {
  border: 1px solid var(--color-border-muted);
  border-radius: 1cqh;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-action-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 82px;
  padding: 16px 18px;
  color: inherit;
  text-decoration: none;
  transition: background-color 120ms ease;
}

.quick-action-link:nth-child(odd) {
  border-right: 1px solid var(--color-border-muted);
}

.quick-action-link:nth-child(n + 3) {
  border-top: 1px solid var(--color-border-muted);
}

.quick-action-link:hover {
  background-color: var(--color-surface);
}

.quick-action-title {
  font-size: 0.9375rem;
  display: block;
}

.quick-action-description {
  margin-top: 6px;
  line-height: 1.4;
}

.quick-action-chevron {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: var(--color-primary);
  stroke-width: 1.75;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

@media (max-width: 960px) {
  .service-grid,
  .quick-actions-grid {
    grid-template-columns: 1fr;
  }

  .service-tile {
    min-height: auto;
  }

  .quick-action-link:nth-child(odd) {
    border-right: 0;
  }

  .quick-action-link + .quick-action-link {
    border-top: 1px solid var(--color-border-muted);
  }
}

@media (max-width: 720px) {
  .service-tile-header,
  .attention-link {
    flex-direction: column;
    align-items: flex-start;
  }

  .service-metric {
    flex-direction: column;
    align-items: flex-start;
  }

  .quick-action-link {
    align-items: flex-start;
  }
}
</style>
