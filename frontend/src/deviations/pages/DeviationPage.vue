<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DeviationDetailPanel from '@/deviations/components/DeviationDetailPanel.vue'
import { createDeviationDataset } from '@/deviations/model/deviation.mock'
import BaseButton from '@/shared/components/BaseButton.vue'
import type {
  DeviationListItem,
  DeviationServiceArea,
  DeviationSeverity,
  DeviationStatus,
} from '@/deviations/model/deviation.types'

const route = useRoute()
const router = useRouter()
const searchQuery = ref('')
const activeFilter = ref<'ALL' | 'OPEN' | 'RECENT'>('ALL')
const deviations = ref(createDeviationDataset())

const filterOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'RECENT', label: 'Recent' },
] as const

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

const serviceDeviations = computed<DeviationListItem[]>(() => {
  return [...deviations.value[currentServiceArea.value]].sort(
    (left, right) => new Date(right.reportedAt).getTime() - new Date(left.reportedAt).getTime(),
  )
})

const selectedDeviationId = computed(() => {
  return typeof route.query.deviationId === 'string' ? route.query.deviationId : null
})

const selectedDeviation = computed(() => {
  if (!selectedDeviationId.value) {
    return null
  }

  return serviceDeviations.value.find((deviation) => deviation.id === selectedDeviationId.value) ?? null
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

function goToDeviationPage() {
  const routeName = typeof route.name === 'string' ? route.name : ''

  if (routeName.startsWith('ik-alkohol-')) {
    router.push({ name: 'ik-alkohol-deviation-form' })
    return
  }

  router.push({ name: 'ik-mat-deviation-form' })
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

function handleDeviationUpdate(updatedDeviation: DeviationListItem) {
  deviations.value = {
    ...deviations.value,
    [updatedDeviation.serviceArea]: deviations.value[updatedDeviation.serviceArea].map((deviation) =>
      deviation.id === updatedDeviation.id ? updatedDeviation : deviation,
    ),
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

function formatStatus(status: DeviationStatus) {
  return status.toLowerCase().replace('_', ' ')
}

function formatSeverity(severity: DeviationSeverity) {
  return severity.toLowerCase()
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && selectedDeviation.value) {
    void clearSelectedDeviation()
  }
}

watch([currentServiceArea, selectedDeviationId], async () => {
  if (selectedDeviationId.value && !selectedDeviation.value) {
    await clearSelectedDeviation()
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <div class="deviation-page">
    <div
      class="deviation-content"
      :class="{ 'deviation-content--with-detail': Boolean(selectedDeviation) }"
    >
      <div class="deviation-primary">
        <header class="page-header">
          <div class="page-header-copy">
            <h1>Deviations</h1>
            <p class="page-subtitle">{{ pageSubtitle }}</p>
          </div>

          <BaseButton class="add-button" type="button" @click="goToDeviationPage">
            <span class="add-button-content">
              <svg aria-hidden="true" class="add-button-icon" viewBox="0 0 20 20">
                <path
                  d="M10 4.5v11"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2.5"
                />
                <path
                  d="M4.5 10h11"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2.5"
                />
              </svg>
              <span>Add deviation</span>
            </span>
          </BaseButton>
        </header>

        <section aria-label="Deviation overview" class="list-panel">
          <div class="list-toolbar">
            <div class="search-field">
              <label class="search-label" for="deviation-search">Search</label>
              <input
                id="deviation-search"
                v-model="searchQuery"
                class="search-input"
                placeholder="Search deviations"
                type="search"
              />
            </div>

            <div aria-label="Deviation filters" class="filter-group">
              <button
                v-for="filterOption in filterOptions"
                :key="filterOption.value"
                :data-active="activeFilter === filterOption.value"
                class="filter-chip"
                type="button"
                @click="activeFilter = filterOption.value"
              >
                {{ filterOption.label }}
              </button>
            </div>
          </div>

          <ul v-if="filteredDeviations.length > 0" class="deviation-list">
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

      <aside v-if="selectedDeviation" class="detail-panel-shell">
        <DeviationDetailPanel
          :deviation="selectedDeviation"
          :show-close-button="true"
          @close="clearSelectedDeviation"
          @update="handleDeviationUpdate"
        />
      </aside>
    </div>

    <div
      v-if="selectedDeviation"
      class="detail-drawer-backdrop"
      aria-hidden="true"
      @click="clearSelectedDeviation"
    />

    <aside
      v-if="selectedDeviation"
      class="detail-drawer"
      aria-label="Selected deviation details"
      @click.stop
    >
      <DeviationDetailPanel
        :deviation="selectedDeviation"
        :show-close-button="true"
        @close="clearSelectedDeviation"
        @update="handleDeviationUpdate"
      />
    </aside>
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
  gap: 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header-copy h1,
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
  font-weight: bold;
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
  gap: 16px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0;
}

.search-field {
  display: flex;
  min-width: min(100%, 320px);
  flex: 1 1 320px;
  flex-direction: column;
  gap: 6px;
}

.search-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.search-input {
  width: 100%;
  padding: 0.875rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.search-input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-color: transparent;
}

.filter-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: 0.875rem;
  cursor: pointer;
}

.filter-chip[data-active='true'] {
  border-color: var(--color-text-primary);
  color: var(--color-text-primary);
}

.filter-chip:hover {
  color: var(--color-text-primary);
}

.deviation-list {
  margin: 0;
  list-style: none;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding: 0 4px 0 0;
}

.deviation-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.deviation-row:hover {
  border-color: var(--color-primary);
}

.deviation-row:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.deviation-row[data-selected='true'] {
  border-color: var(--color-primary);
  background-color: var(--color-surface);
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
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
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

.detail-drawer-backdrop,
.detail-drawer {
  display: none;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }

  .add-button {
    width: 100%;
  }

  .list-toolbar {
    align-items: stretch;
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

  .detail-drawer-backdrop {
    position: fixed;
    inset: 0;
    z-index: 20;
    display: block;
    background-color: rgba(15, 23, 42, 0.32);
  }

  .detail-drawer {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 21;
    display: block;
    width: min(100%, 420px);
    height: 100vh;
    padding: 16px;
    box-sizing: border-box;
    overflow-y: auto;
  }
}
</style>
