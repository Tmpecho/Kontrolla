<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { useAuthStore } from '@/auth/model/auth.store'
import TemperatureSparkline from '@/ik-mat/components/TemperatureSparkline.vue'
import { createTemperatureLog, listTemperatureUnits } from '@/ik-mat/api/temperature.api'
import AppOverlay from '@/shared/components/overlay/AppOverlay.vue'
import type {
  TemperatureAlertState,
  TemperatureLogEntry,
  TemperatureLoggingStatus,
  TemperatureUnit,
  TemperatureUnitListItem,
} from '@/ik-mat/model/temperature.types'
import {
  formatTemperatureAlertState,
  formatTemperatureUnitType,
  getTemperatureSummary,
  getTemperatureUnitsWithStatus,
  isTemperatureWithinRange,
} from '@/ik-mat/model/temperature.utils'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

type TemperatureFilter = 'ALL' | 'OVERDUE' | 'DUE_SOON'
type SaveState = 'IDLE' | 'SAVING'
type TemperatureSaveResult = {
  unitId: string
  measuredAt: string
  loggedByName: string
  temperatureCelsius: number
  note: string | null
  isOutOfRange: boolean
}

const authStore = useAuthStore()

const units = ref<TemperatureUnit[]>([])
const now = ref(new Date())
const searchQuery = ref('')
const activeFilter = ref<TemperatureFilter>('ALL')
const editingUnitId = ref<string | null>(null)
const draftTemperature = ref('')
const draftMeasuredAt = ref('')
const draftNote = ref('')
const draftError = ref<string | null>(null)
const saveState = ref<SaveState>('IDLE')
const latestSaveResult = ref<TemperatureSaveResult | null>(null)
const isMobileEditor = ref(false)
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

let nowRefreshTimer: number | null = null
let requestSequence = 0

const filterOptions: Array<{ value: TemperatureFilter; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'DUE_SOON', label: 'Due soon' },
]

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(
  () => authStore.appContext?.establishmentId ?? appEnv.defaultEstablishmentId ?? null,
)

const hasTemperatureContext = computed(() => Boolean(organizationId.value && establishmentId.value))

const missingContextMessage = computed(() => {
  if (hasTemperatureContext.value) {
    return null
  }

  if (!appEnv.isDevelopment) {
    return 'Temperature logs cannot be loaded until organization and establishment context is available.'
  }

  return 'Set VITE_DEFAULT_ORGANIZATION_ID and VITE_DEFAULT_ESTABLISHMENT_ID or sign in with an organization context to load temperature units.'
})

const unitsWithStatus = computed<TemperatureUnitListItem[]>(() => {
  return [...getTemperatureUnitsWithStatus(units.value, now.value)].sort((left, right) => {
    const statusOrder = getAlertSortOrder(left.alertState) - getAlertSortOrder(right.alertState)

    if (statusOrder !== 0) {
      return statusOrder
    }

    const dueOrder = left.nextDueAt.getTime() - right.nextDueAt.getTime()

    if (dueOrder !== 0) {
      return dueOrder
    }

    return left.name.localeCompare(right.name, 'nb-NO')
  })
})

const summary = computed(() => getTemperatureSummary(units.value, now.value))

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
    case 'OVERDUE':
      items = items.filter((unit) => unit.loggingStatus === 'OVERDUE')
      break
    case 'DUE_SOON':
      items = items.filter((unit) => unit.loggingStatus === 'DUE_SOON')
      break
    default:
      break
  }

  return items
})

const editingUnit = computed(() => {
  if (!editingUnitId.value) {
    return null
  }

  return unitsWithStatus.value.find((unit) => unit.id === editingUnitId.value) ?? null
})

const currentSaveResult = computed(() => {
  if (!latestSaveResult.value) {
    return null
  }

  return latestSaveResult.value
})

const showSummary = computed(() => {
  return !missingContextMessage.value && !isLoading.value && !errorMessage.value
})

const emptyStateMessage = computed(() => {
  if (searchQuery.value.trim()) {
    return 'No temperature units matched your search.'
  }

  if (activeFilter.value === 'OVERDUE') {
    return 'No units are overdue right now.'
  }

  if (activeFilter.value === 'DUE_SOON') {
    return 'No units are due soon right now.'
  }

  return 'No temperature units configured yet.'
})

function getAlertSortOrder(alertState: TemperatureAlertState): number {
  switch (alertState) {
    case 'OUT_OF_RANGE':
      return 0
    case 'OVERDUE':
      return 1
    case 'DUE_SOON':
      return 2
    case 'NO_READING':
      return 3
    case 'DUE_LATER_TODAY':
      return 4
    default:
      return 5
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

function formatTime(value: Date): string {
  return new Intl.DateTimeFormat('nb-NO', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function formatDateTimeInputValue(date: Date = new Date()): string {
  const copy = new Date(date)
  copy.setSeconds(0, 0)

  const year = copy.getFullYear()
  const month = String(copy.getMonth() + 1).padStart(2, '0')
  const day = String(copy.getDate()).padStart(2, '0')
  const hours = String(copy.getHours()).padStart(2, '0')
  const minutes = String(copy.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function formatDueMessage(unit: TemperatureUnitListItem): string {
  const nextDueTime = formatTime(unit.nextDueAt)

  switch (unit.loggingStatus) {
    case 'LOGGED_TODAY':
      return `Logged today • next due tomorrow by ${nextDueTime}`
    case 'OVERDUE':
      return `Overdue since ${nextDueTime}`
    case 'DUE_SOON':
      return `Due soon • today by ${nextDueTime}`
    default:
      return `Next due today by ${nextDueTime}`
  }
}

function getDueIndicatorTone(loggingStatus: TemperatureLoggingStatus): string {
  switch (loggingStatus) {
    case 'OVERDUE':
      return 'overdue'
    case 'DUE_SOON':
      return 'soon'
    case 'DUE_LATER_TODAY':
      return 'later'
    default:
      return 'logged'
  }
}

function resetDraft(): void {
  draftTemperature.value = ''
  draftMeasuredAt.value = formatDateTimeInputValue(new Date())
  draftNote.value = ''
  draftError.value = null
  saveState.value = 'IDLE'
}

function openEditor(unitId: string): void {
  editingUnitId.value = unitId
  draftError.value = null
  saveState.value = 'IDLE'
  draftTemperature.value = ''
  draftMeasuredAt.value = formatDateTimeInputValue(new Date())
  draftNote.value = ''
}

function closeEditor(): void {
  editingUnitId.value = null
  resetDraft()
}

function updateViewportMode(): void {
  isMobileEditor.value = window.innerWidth <= 720
}

function handleEscape(event: KeyboardEvent): void {
  if (event.key === 'Escape' && editingUnitId.value && !isMobileEditor.value) {
    closeEditor()
  }
}

function buildDeviationQuery(unit: TemperatureUnitListItem) {
  if (!currentSaveResult.value || currentSaveResult.value.unitId !== unit.id) {
    return undefined
  }

  return {
    title: `Temperature deviation - ${unit.name}`,
    category: 'Temperature',
    description: [
      `Unit: ${unit.name}`,
      `Location: ${unit.location}`,
      `Reading: ${formatTemperature(currentSaveResult.value.temperatureCelsius)}`,
      `Acceptable range: ${formatRange(unit)}`,
      `Measured at: ${formatDateTime(currentSaveResult.value.measuredAt)}`,
      `Logged by: ${currentSaveResult.value.loggedByName}`,
      currentSaveResult.value.note ? `Note: ${currentSaveResult.value.note}` : null,
    ]
      .filter(Boolean)
      .join('\n'),
  }
}

function requiresFollowUpNote(unit: TemperatureUnitListItem): boolean {
  const parsedTemperature = Number.parseFloat(draftTemperature.value.replace(',', '.'))

  if (Number.isNaN(parsedTemperature)) {
    return false
  }

  return !isTemperatureWithinRange(unit, parsedTemperature)
}

function getSaveFeedback(unitId: string) {
  if (!currentSaveResult.value || currentSaveResult.value.unitId !== unitId) {
    return null
  }

  return currentSaveResult.value
}

async function saveTemperatureLog(unit: TemperatureUnitListItem): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    draftError.value = 'Temperature context is missing.'
    return
  }

  const parsedTemperature = Number.parseFloat(draftTemperature.value.replace(',', '.'))

  if (Number.isNaN(parsedTemperature)) {
    draftError.value = 'Enter a valid temperature.'
    return
  }

  const measuredAt = new Date(draftMeasuredAt.value)

  if (Number.isNaN(measuredAt.getTime())) {
    draftError.value = 'Select a valid date and time.'
    return
  }

  const requiresFollowUpNote = !isTemperatureWithinRange(unit, parsedTemperature)

  if (requiresFollowUpNote && !draftNote.value.trim()) {
    draftError.value = 'Add a follow-up note for out-of-range readings.'
    return
  }

  draftError.value = null
  saveState.value = 'SAVING'

  const measuredAtIso = measuredAt.toISOString()
  const normalizedNote = draftNote.value.trim() || null

  try {
    const createdLog = await createTemperatureLog({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
      temperatureUnitId: unit.id,
      temperatureCelsius: parsedTemperature,
      measuredAt: measuredAtIso,
      note: normalizedNote,
    })

    units.value = units.value.map((existingUnit) => {
      if (existingUnit.id !== unit.id) {
        return existingUnit
      }

      return {
        ...existingUnit,
        logs: prependLog(existingUnit.logs, createdLog),
      }
    })

    latestSaveResult.value = {
      unitId: unit.id,
      measuredAt: createdLog.measuredAt,
      loggedByName: createdLog.loggedByName,
      temperatureCelsius: createdLog.temperatureCelsius,
      note: createdLog.note,
      isOutOfRange: requiresFollowUpNote,
    }

    now.value = new Date()
    closeEditor()
  } catch (error) {
    draftError.value =
      error instanceof ApiError ? error.message : 'Could not save the temperature reading.'
    saveState.value = 'IDLE'
  }
}

function prependLog(logs: TemperatureLogEntry[], createdLog: TemperatureLogEntry): TemperatureLogEntry[] {
  return [createdLog, ...logs.filter((logEntry) => logEntry.id !== createdLog.id)]
}

async function loadTemperatureUnits(): Promise<void> {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value
  const currentRequestId = ++requestSequence

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    units.value = []
    errorMessage.value = null
    isLoading.value = false
    closeEditor()
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const nextUnits = await listTemperatureUnits({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
    })

    if (currentRequestId !== requestSequence) {
      return
    }

    units.value = nextUnits
  } catch (error) {
    if (currentRequestId !== requestSequence) {
      return
    }

    units.value = []
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Could not load temperature units.'
  } finally {
    if (currentRequestId === requestSequence) {
      isLoading.value = false
    }
  }
}

onMounted(() => {
  updateViewportMode()
  window.addEventListener('resize', updateViewportMode)
  document.addEventListener('keydown', handleEscape)
  nowRefreshTimer = window.setInterval(() => {
    now.value = new Date()
  }, 60_000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportMode)
  document.removeEventListener('keydown', handleEscape)

  if (nowRefreshTimer !== null) {
    window.clearInterval(nowRefreshTimer)
  }
})

watch([organizationId, establishmentId], () => {
  void loadTemperatureUnits()
}, { immediate: true })
</script>

<template>
  <div class="temperature-page">
    <header class="page-header">
      <div class="page-header-copy">
        <h1>Temperature</h1>
        <p class="page-subtitle">
          Log fridge and freezer temperatures quickly, keep track of what is due next, and follow
          up immediately when a reading falls outside the acceptable range.
        </p>
      </div>
    </header>

    <section v-if="showSummary" class="summary-grid" aria-label="Temperature overview">
      <article class="summary-card summary-card-attention">
        <p class="summary-label">Needs attention</p>
        <p class="summary-value">{{ summary.needsAttentionCount }}</p>
        <p class="summary-support">
          {{
            summary.needsAttentionCount === 1
              ? '1 unit needs follow-up right now.'
              : `${summary.needsAttentionCount} units need follow-up right now.`
          }}
        </p>
      </article>

      <article class="summary-card summary-card-overdue">
        <p class="summary-label">Overdue now</p>
        <p class="summary-value">{{ summary.overdueNowCount }}</p>
        <p class="summary-support">
          {{
            summary.overdueNowCount === 1
              ? '1 unit has passed its logging time.'
              : `${summary.overdueNowCount} units have passed their logging time.`
          }}
        </p>
      </article>

      <article class="summary-card summary-card-soon">
        <p class="summary-label">Due soon</p>
        <p class="summary-value">{{ summary.dueSoonCount }}</p>
        <p class="summary-support">
          {{
            summary.dueSoonCount === 1
              ? '1 unit is due within the next two hours.'
              : `${summary.dueSoonCount} units are due within the next two hours.`
          }}
        </p>
      </article>

      <article class="summary-card summary-card-range">
        <p class="summary-label">Latest reading in range</p>
        <p class="summary-value">{{ summary.latestInRangeCount }}</p>
        <p class="summary-support">
          {{
            summary.latestInRangeCount === 1
              ? '1 unit has a latest reading within threshold.'
              : `${summary.latestInRangeCount} units have latest readings within threshold.`
          }}
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

      <div v-if="missingContextMessage" class="empty-state">
        <p>{{ missingContextMessage }}</p>
      </div>

      <div v-else-if="isLoading" class="empty-state">
        <p>Loading temperature units...</p>
      </div>

      <div v-else-if="errorMessage" class="empty-state">
        <p>{{ errorMessage }}</p>
      </div>

      <ul v-else-if="filteredUnits.length > 0" class="temperature-list">
        <li v-for="unit in filteredUnits" :key="unit.id" class="temperature-list-item">
          <article class="temperature-row">
            <div class="temperature-primary">
              <div class="temperature-heading">
                <h2>{{ unit.name }}</h2>
                <span class="unit-type">{{ formatTemperatureUnitType(unit.type) }}</span>
              </div>
              <p class="temperature-location">{{ unit.location }}</p>
              <p class="temperature-range">Acceptable range: {{ formatRange(unit) }}</p>
              <p
                class="due-indicator"
                :data-due-tone="getDueIndicatorTone(unit.loggingStatus)"
              >
                {{ formatDueMessage(unit) }}
              </p>
            </div>

            <div class="temperature-reading">
              <p class="data-label">Latest reading</p>
              <p v-if="unit.latestLog" class="data-value">
                {{ formatTemperature(unit.latestLog.temperatureCelsius) }}
              </p>
              <p v-else class="data-value">No reading yet</p>
              <p v-if="unit.latestLog" class="data-support">
                {{ formatDateTime(unit.latestLog.measuredAt) }}
              </p>
              <p v-if="unit.latestLog" class="data-support">
                Logged by {{ unit.latestLog.loggedByName }}
              </p>
              <p v-if="unit.latestLog?.note" class="data-note">{{ unit.latestLog.note }}</p>
            </div>

            <div class="temperature-trend">
              <p class="data-label">Last 7 readings</p>
              <TemperatureSparkline
                :alert-state="unit.alertState"
                :logs="unit.logs"
                :maximum-temperature="unit.maximumTemperature"
                :minimum-temperature="unit.minimumTemperature"
              />
            </div>

            <div class="temperature-status">
              <p class="data-label">Primary status</p>
              <span class="status-badge" :data-state="unit.alertState">
                {{ formatTemperatureAlertState(unit.alertState) }}
              </span>
            </div>

            <div class="temperature-actions">
              <button type="button" class="row-action" @click="openEditor(unit.id)">
                Log reading
              </button>

              <div
                v-if="getSaveFeedback(unit.id)"
                class="save-feedback"
                :data-tone="getSaveFeedback(unit.id)?.isOutOfRange ? 'critical' : 'success'"
              >
                <p class="save-feedback-title">
                  Saved {{ formatTime(new Date(getSaveFeedback(unit.id)!.measuredAt)) }}
                </p>
                <p class="save-feedback-copy">
                  Logged by {{ getSaveFeedback(unit.id)!.loggedByName }}
                </p>

                <RouterLink
                  v-if="getSaveFeedback(unit.id)?.isOutOfRange"
                  :to="{ name: 'ik-mat-deviation-form', query: buildDeviationQuery(unit) }"
                  class="deviation-link"
                >
                  Report deviation
                </RouterLink>
              </div>
            </div>
          </article>

          <div v-if="editingUnitId === unit.id && !isMobileEditor" class="inline-editor">
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

              <label class="editor-field">
                <span>Measured at</span>
                <input
                  v-model="draftMeasuredAt"
                  class="editor-input"
                  type="datetime-local"
                />
              </label>

              <label class="editor-field editor-field-note">
                <span>
                  {{ requiresFollowUpNote(unit) ? 'Follow-up note (required if out of range)' : 'Note (optional)' }}
                </span>
                <textarea
                  v-model="draftNote"
                  class="editor-input editor-textarea"
                  placeholder="Document what was checked or what was done next"
                  rows="3"
                />
              </label>
            </div>

            <p class="editor-help">
              Acceptable range: {{ formatRange(unit) }}. Out-of-range readings require a note.
            </p>
            <p v-if="draftError" class="editor-error">{{ draftError }}</p>

            <div class="editor-actions">
              <button
                type="button"
                class="editor-button editor-button-primary"
                :disabled="saveState === 'SAVING'"
                @click="saveTemperatureLog(unit)"
              >
                {{ saveState === 'SAVING' ? 'Saving...' : 'Save reading' }}
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

    <AppOverlay
      :open="Boolean(editingUnit) && isMobileEditor"
      aria-label="Log temperature reading"
      variant="sheet-bottom"
      @close="closeEditor"
    >
      <section v-if="editingUnit" class="editor-sheet">
          <div class="editor-sheet-header">
            <div class="editor-sheet-copy">
              <h2>{{ editingUnit.name }}</h2>
              <p>{{ editingUnit.location }}</p>
            </div>

            <button type="button" class="editor-close-button" @click="closeEditor">Close</button>
          </div>

          <div class="editor-grid editor-grid-mobile">
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

            <label class="editor-field">
              <span>Measured at</span>
              <input
                v-model="draftMeasuredAt"
                class="editor-input"
                type="datetime-local"
              />
            </label>

            <label class="editor-field editor-field-note">
              <span>
                {{
                  requiresFollowUpNote(editingUnit)
                    ? 'Follow-up note (required if out of range)'
                    : 'Note (optional)'
                }}
              </span>
              <textarea
                v-model="draftNote"
                class="editor-input editor-textarea"
                placeholder="Document what was checked or what was done next"
                rows="4"
              />
            </label>
          </div>

          <p class="editor-help">
            Acceptable range: {{ formatRange(editingUnit) }}. Out-of-range readings require a
            note.
          </p>
          <p v-if="draftError" class="editor-error">{{ draftError }}</p>

          <div class="editor-actions">
            <button
              type="button"
              class="editor-button editor-button-primary"
              :disabled="saveState === 'SAVING'"
              @click="saveTemperatureLog(editingUnit)"
            >
              {{ saveState === 'SAVING' ? 'Saving...' : 'Save reading' }}
            </button>
            <button type="button" class="editor-button" @click="closeEditor">Cancel</button>
          </div>
      </section>
    </AppOverlay>
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
.editor-error,
.save-feedback-title,
.save-feedback-copy,
.editor-sheet-copy h2,
.editor-sheet-copy p {
  margin: 0;
}

.page-subtitle {
  max-width: 72ch;
  color: var(--color-text-secondary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas:
    'label value'
    'support value';
  align-items: start;
  gap: 2px 12px;
  padding: 12px 14px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.summary-card-attention {
  background-color: color-mix(in srgb, var(--color-critical) 8%, var(--color-container));
}

.summary-card-overdue {
  background-color: color-mix(in srgb, var(--color-warning) 10%, var(--color-container));
}

.summary-card-soon {
  background-color: color-mix(in srgb, var(--color-warning) 6%, var(--color-container));
}

.summary-card-range {
  background-color: color-mix(in srgb, var(--color-primary) 8%, var(--color-container));
}

.summary-label {
  grid-area: label;
  color: var(--color-text-secondary);
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summary-value {
  grid-area: value;
  align-self: center;
  font-size: 1.35rem;
  font-weight: 700;
  line-height: 1;
}

.summary-support {
  grid-area: support;
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
  line-height: 1.3;
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

.search-label,
.editor-field span {
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

.editor-textarea {
  min-height: 108px;
  resize: vertical;
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
  grid-template-columns: minmax(220px, 2fr) minmax(160px, 1fr) minmax(160px, 1fr) minmax(
      130px,
      0.9fr
    ) minmax(170px, 1fr);
  grid-template-areas: 'primary reading trend status actions';
  gap: 16px;
  align-items: center;
  padding: 16px 20px;
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
.temperature-status,
.temperature-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.temperature-primary {
  grid-area: primary;
}

.temperature-reading {
  grid-area: reading;
}

.temperature-trend {
  grid-area: trend;
}

.temperature-status {
  grid-area: status;
}

.temperature-actions {
  grid-area: actions;
}

.temperature-location,
.temperature-range,
.due-indicator,
.data-support,
.data-note {
  color: var(--color-text-secondary);
}

.due-indicator {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border-radius: 4px;
  padding: 0.22rem 0.45rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.due-indicator[data-due-tone='logged'] {
  background-color: color-mix(in srgb, var(--color-primary) 8%, var(--color-container));
  color: var(--color-primary);
}

.due-indicator[data-due-tone='later'] {
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
}

.due-indicator[data-due-tone='soon'] {
  background-color: color-mix(in srgb, var(--color-warning) 12%, var(--color-container));
  color: #b45309;
}

.due-indicator[data-due-tone='overdue'] {
  background-color: color-mix(in srgb, var(--color-critical) 10%, var(--color-container));
  color: var(--color-critical);
}

.data-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.data-value {
  font-size: 1rem;
  font-weight: 700;
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

.status-badge[data-state='LOGGED_TODAY'] {
  background-color: color-mix(in srgb, var(--color-primary) 8%, var(--color-container));
  color: var(--color-primary);
}

.status-badge[data-state='DUE_LATER_TODAY'],
.status-badge[data-state='NO_READING'] {
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
}

.status-badge[data-state='DUE_SOON'],
.status-badge[data-state='OVERDUE'] {
  background-color: color-mix(in srgb, var(--color-warning) 14%, var(--color-container));
  color: #b45309;
}

.status-badge[data-state='OUT_OF_RANGE'] {
  background-color: color-mix(in srgb, var(--color-critical) 14%, var(--color-container));
  color: var(--color-critical);
}

.temperature-actions {
  align-items: flex-start;
}

.row-action,
.editor-button,
.editor-close-button {
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  padding: 0.58rem 0.8rem;
  font-size: 0.875rem;
  cursor: pointer;
}

.row-action:hover,
.editor-button:hover,
.editor-close-button:hover {
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

.editor-button:disabled {
  cursor: wait;
  opacity: 0.75;
}

.save-feedback {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 4px;
  background-color: var(--color-surface);
}

.save-feedback[data-tone='success'] {
  border-left: 3px solid var(--color-primary);
}

.save-feedback[data-tone='critical'] {
  border-left: 3px solid var(--color-critical);
}

.save-feedback-title {
  font-size: 0.875rem;
  font-weight: 700;
}

.save-feedback-copy {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.deviation-link {
  width: fit-content;
  color: var(--color-primary);
  font-size: 0.875rem;
  text-decoration: none;
}

.deviation-link:hover {
  text-decoration: underline;
}

.inline-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 20px 16px;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(160px, 220px) minmax(220px, 280px) minmax(0, 1fr);
  gap: 12px;
}

.editor-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.editor-field-note {
  grid-column: span 3;
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

.editor-sheet {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 16px 24px;
  border-top: 1px solid var(--color-border-muted);
  border-radius: 4px 4px 0 0;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
}

.editor-sheet-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.editor-sheet-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.editor-sheet-copy p {
  color: var(--color-text-secondary);
}

.editor-grid-mobile {
  grid-template-columns: 1fr;
}

.editor-grid-mobile .editor-field-note {
  grid-column: auto;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .temperature-row {
    grid-template-columns: minmax(220px, 2fr) minmax(170px, 1fr) minmax(160px, 0.9fr);
    grid-template-areas:
      'primary reading status'
      'primary trend actions';
    align-items: start;
  }
}

@media (max-width: 920px) {
  .page-header,
  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }

  .editor-field-note {
    grid-column: auto;
  }

  .temperature-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-areas:
      'primary reading'
      'trend status'
      'actions actions';
  }

  .temperature-actions {
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .summary-grid,
  .temperature-row {
    grid-template-columns: 1fr;
  }

  .temperature-actions {
    align-items: stretch;
  }

  .row-action {
    width: 100%;
  }
}
</style>
