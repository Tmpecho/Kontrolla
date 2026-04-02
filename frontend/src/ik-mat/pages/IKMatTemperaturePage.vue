<script setup lang="ts">
import { computed, ref } from 'vue'

import TemperatureSparkline from '@/ik-mat/components/TemperatureSparkline.vue'
import { createTemperatureUnits } from '@/ik-mat/model/temperature.mock'
import type {
  TemperatureStatus,
  TemperatureUnitListItem,
} from '@/ik-mat/model/temperature.types'
import {
  formatTemperatureStatus,
  formatTemperatureUnitType,
  getTemperatureSummary,
  getTemperatureUnitsWithStatus,
} from '@/ik-mat/model/temperature.utils'

type TemperatureFilter = 'ALL' | 'ATTENTION' | 'DUE_TODAY' | 'FRIDGES' | 'FREEZERS'

const units = ref(createTemperatureUnits())
const searchQuery = ref('')
const activeFilter = ref<TemperatureFilter>('ALL')
const editingUnitId = ref<string | null>(null)
const draftTemperature = ref('')
const draftNote = ref('')
const draftError = ref<string | null>(null)

const filterOptions: Array<{ value: TemperatureFilter; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'ATTENTION', label: 'Needs attention' },
  { value: 'DUE_TODAY', label: 'Due today' },
  { value: 'FRIDGES', label: 'Fridges' },
  { value: 'FREEZERS', label: 'Freezers' },
]

const unitsWithStatus = computed<TemperatureUnitListItem[]>(() => {
  return [...getTemperatureUnitsWithStatus(units.value)].sort((left, right) => {
    const statusOrder = getStatusSortOrder(left.status) - getStatusSortOrder(right.status)

    if (statusOrder !== 0) {
      return statusOrder
    }

    return left.name.localeCompare(right.name, 'nb-NO')
  })
})

const summary = computed(() => getTemperatureSummary(units.value))

const filteredUnits = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  let items = unitsWithStatus.value.filter((unit) => {
    if (!query) {
      return true
    }

    return [unit.name, unit.location, formatTemperatureUnitType(unit.type)]
      .join(' ')
      .toLowerCase()
      .includes(query)
  })

  switch (activeFilter.value) {
    case 'ATTENTION':
      items = items.filter((unit) => unit.status !== 'IN_RANGE')
      break
    case 'DUE_TODAY':
      items = items.filter((unit) => unit.status === 'OVERDUE')
      break
    case 'FRIDGES':
      items = items.filter((unit) => unit.type === 'FRIDGE')
      break
    case 'FREEZERS':
      items = items.filter((unit) => unit.type === 'FREEZER')
      break
    default:
      break
  }

  return items
})

const emptyStateMessage = computed(() => {
  if (searchQuery.value.trim()) {
    return 'No temperature units matched your search.'
  }

  if (activeFilter.value === 'ATTENTION') {
    return 'No units need attention right now.'
  }

  if (activeFilter.value === 'DUE_TODAY') {
    return 'No units are due for logging right now.'
  }

  return 'No temperature units configured yet.'
})

function getStatusSortOrder(status: TemperatureStatus): number {
  switch (status) {
    case 'OUT_OF_RANGE':
      return 0
    case 'OVERDUE':
      return 1
    default:
      return 2
  }
}

function formatTemperature(value: number): string {
  return `${new Intl.NumberFormat('nb-NO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1,
  }).format(value)}°C`
}

function formatRange(unit: TemperatureUnitListItem): string {
  return `${formatTemperature(unit.minimumTemperature)} to ${formatTemperature(unit.maximumTemperature)}`
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function openEditor(unitId: string): void {
  editingUnitId.value = unitId
  draftTemperature.value = ''
  draftNote.value = ''
  draftError.value = null
}

function closeEditor(): void {
  editingUnitId.value = null
  draftTemperature.value = ''
  draftNote.value = ''
  draftError.value = null
}

function saveTemperatureLog(unitId: string): void {
  const parsedTemperature = Number.parseFloat(draftTemperature.value.replace(',', '.'))

  if (Number.isNaN(parsedTemperature)) {
    draftError.value = 'Enter a valid temperature.'
    return
  }

  units.value = units.value.map((unit) => {
    if (unit.id !== unitId) {
      return unit
    }

    return {
      ...unit,
      logs: [
        {
          id: `${unitId}-${Date.now()}`,
          measuredAt: new Date().toISOString(),
          temperatureCelsius: parsedTemperature,
          note: draftNote.value.trim() || null,
        },
        ...unit.logs,
      ],
    }
  })

  closeEditor()
}
</script>

<template>
  <div class="temperature-page">
    <header class="page-header">
      <div class="page-header-copy">
        <h1>Temperature</h1>
        <p class="page-subtitle">
          Log fridge and freezer temperatures, track thresholds, and spot issues before they become
          deviations.
        </p>
      </div>
    </header>

    <section class="summary-grid" aria-label="Temperature overview">
      <article class="summary-card summary-card-attention">
        <p class="summary-label">Needs attention</p>
        <p class="summary-value">{{ summary.needsAttentionCount }}</p>
        <p class="summary-support">
          {{ summary.needsAttentionCount === 1 ? '1 unit requires follow-up.' : `${summary.needsAttentionCount} units require follow-up.` }}
        </p>
      </article>

      <article class="summary-card summary-card-due">
        <p class="summary-label">Due today</p>
        <p class="summary-value">{{ summary.dueTodayCount }}</p>
        <p class="summary-support">
          {{ summary.dueTodayCount === 1 ? '1 unit still needs a reading today.' : `${summary.dueTodayCount} units still need readings today.` }}
        </p>
      </article>

      <article class="summary-card summary-card-range">
        <p class="summary-label">In range now</p>
        <p class="summary-value">{{ summary.inRangeCount }}</p>
        <p class="summary-support">
          {{ summary.inRangeCount === 1 ? '1 unit is currently within threshold.' : `${summary.inRangeCount} units are currently within threshold.` }}
        </p>
      </article>
    </section>

    <section aria-label="Temperature log units" class="list-panel">
      <div class="list-toolbar">
        <div class="search-field">
          <label class="search-label" for="temperature-search">Search</label>
          <input
            id="temperature-search"
            v-model="searchQuery"
            class="search-input"
            placeholder="Search units"
            type="search"
          />
        </div>

        <div aria-label="Temperature filters" class="filter-group">
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

      <ul v-if="filteredUnits.length > 0" class="temperature-list">
        <li v-for="unit in filteredUnits" :key="unit.id" class="temperature-list-item">
          <article class="temperature-row">
            <div class="temperature-primary">
              <div class="temperature-heading">
                <h2>{{ unit.name }}</h2>
                <span class="unit-type">{{ formatTemperatureUnitType(unit.type) }}</span>
              </div>
              <p class="temperature-location">{{ unit.location }}</p>
              <p class="temperature-range">Acceptable range: {{ formatRange(unit) }}</p>
            </div>

            <div class="temperature-reading">
              <p class="data-label">Latest reading</p>
              <p v-if="unit.latestLog" class="data-value">
                {{ formatTemperature(unit.latestLog.temperatureCelsius) }}
              </p>
              <p v-else class="data-value">No reading</p>
              <p v-if="unit.latestLog" class="data-support">
                {{ formatDateTime(unit.latestLog.measuredAt) }}
              </p>
              <p v-if="unit.latestLog?.note" class="data-note">{{ unit.latestLog.note }}</p>
            </div>

            <div class="temperature-trend">
              <p class="data-label">Trend</p>
              <TemperatureSparkline :logs="unit.logs" :status="unit.status" />
            </div>

            <div class="temperature-status">
              <p class="data-label">Status</p>
              <span class="status-badge" :data-status="unit.status">
                {{ formatTemperatureStatus(unit.status) }}
              </span>
            </div>

            <div class="temperature-actions">
              <button type="button" class="row-action" @click="openEditor(unit.id)">
                Log reading
              </button>
            </div>
          </article>

          <div v-if="editingUnitId === unit.id" class="inline-editor">
            <div class="editor-grid">
              <label class="editor-field">
                <span>Temperature</span>
                <input
                  v-model="draftTemperature"
                  class="editor-input"
                  inputmode="decimal"
                  placeholder="e.g. 3.4"
                  type="text"
                />
              </label>

              <label class="editor-field editor-field-note">
                <span>Note (optional)</span>
                <input
                  v-model="draftNote"
                  class="editor-input"
                  placeholder="Add a note if something needs follow-up"
                  type="text"
                />
              </label>
            </div>

            <p class="editor-help">Acceptable range: {{ formatRange(unit) }}</p>
            <p v-if="draftError" class="editor-error">{{ draftError }}</p>

            <div class="editor-actions">
              <button type="button" class="editor-button editor-button-primary" @click="saveTemperatureLog(unit.id)">
                Save
              </button>
              <button type="button" class="editor-button" @click="closeEditor">Cancel</button>
            </div>
          </div>
        </li>
      </ul>

      <div v-else class="empty-state">
        <p>{{ emptyStateMessage }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.temperature-page {
  display: flex;
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
.summary-label,
.summary-value,
.summary-support,
.data-label,
.data-value,
.data-support,
.data-note,
.temperature-location,
.temperature-range,
.empty-state p,
.editor-help,
.editor-error {
  margin: 0;
}

.page-subtitle {
  max-width: 72ch;
  color: var(--color-text-secondary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.summary-card-attention {
  background-color: color-mix(in srgb, var(--color-critical) 8%, var(--color-container));
}

.summary-card-due {
  background-color: color-mix(in srgb, var(--color-warning) 8%, var(--color-container));
}

.summary-card-range {
  background-color: color-mix(in srgb, var(--color-primary) 8%, var(--color-container));
}

.summary-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summary-value {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
}

.summary-support {
  color: var(--color-text-secondary);
}

.list-panel {
  overflow: hidden;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.list-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border-muted);
}

.search-field {
  display: flex;
  min-width: 220px;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.search-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.search-input,
.editor-input {
  width: 100%;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.875rem 1rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background-color: var(--color-container);
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
  padding: 0.65rem 0.875rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.filter-chip[data-active='true'] {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background-color: color-mix(in srgb, var(--color-primary) 6%, var(--color-container));
}

.temperature-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.temperature-list-item + .temperature-list-item {
  border-top: 1px solid var(--color-border-muted);
}

.temperature-row {
  display: grid;
  grid-template-columns: minmax(220px, 2fr) minmax(140px, 1fr) minmax(140px, 1fr) auto auto;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
}

.temperature-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.temperature-heading h2 {
  margin: 0;
  font-size: 1rem;
}

.unit-type {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.2rem 0.45rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  background-color: var(--color-surface);
}

.temperature-primary,
.temperature-reading,
.temperature-trend,
.temperature-status {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.temperature-location,
.temperature-range,
.data-support,
.data-note {
  color: var(--color-text-secondary);
}

.data-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.data-value {
  font-size: 1.125rem;
  font-weight: 700;
}

.temperature-actions {
  display: flex;
  justify-content: flex-end;
}

.row-action,
.editor-button {
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  padding: 0.7rem 0.95rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.row-action:hover,
.editor-button:hover {
  background-color: var(--color-surface);
}

.editor-button-primary {
  border-color: var(--color-primary);
  background-color: var(--color-primary);
  color: var(--color-white);
}

.editor-button-primary:hover {
  background-color: color-mix(in srgb, var(--color-primary) 88%, black);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  border-radius: 999px;
  padding: 0.35rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.status-badge[data-status='IN_RANGE'] {
  background-color: color-mix(in srgb, var(--color-success) 14%, var(--color-container));
  color: var(--color-success);
}

.status-badge[data-status='OUT_OF_RANGE'] {
  background-color: color-mix(in srgb, var(--color-critical) 14%, var(--color-container));
  color: var(--color-critical);
}

.status-badge[data-status='OVERDUE'] {
  background-color: color-mix(in srgb, var(--color-warning) 14%, var(--color-container));
  color: #b45309;
}

.inline-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 24px 20px;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(180px, 220px) minmax(0, 1fr);
  gap: 12px;
}

.editor-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.editor-field span {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.editor-help {
  color: var(--color-text-secondary);
}

.editor-error {
  color: var(--color-critical);
}

.editor-actions {
  display: flex;
  gap: 10px;
}

.empty-state {
  padding: 32px 24px;
  color: var(--color-text-secondary);
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .temperature-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .temperature-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .page-header,
  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .temperature-row,
  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
