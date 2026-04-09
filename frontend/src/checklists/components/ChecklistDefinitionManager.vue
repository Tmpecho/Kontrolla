<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ArrowDown, ArrowUp, ChevronDown, ChevronUp, Trash2 } from 'lucide-vue-next'

import {
  createChecklistDefinition,
  listChecklistDefinitions,
  updateChecklistDefinition,
  type ChecklistDefinitionScheduleInput,
  type ChecklistDefinitionTaskInput,
  type UpsertChecklistDefinitionInput,
} from '@/checklists/api/checklist-definitions.api'
import type {
  ChecklistDefinition,
  ChecklistDefinitionSchedule,
  ChecklistScheduleType,
  ChecklistTaskKind,
} from '@/checklists/model/checklist-definitions.types'
import type { ChecklistServiceArea } from '@/checklists/model/checklist.types'
import { ApiError } from '@/shared/api/http'

type TaskDraft = {
  id: string
  title: string
  details: string
  taskKind: ChecklistTaskKind
  required: boolean
  measurementUnit: string
  minimumAllowedValue: string
  maximumAllowedValue: string
}

type ScheduleDraft = {
  id: string
  scheduleType: ChecklistScheduleType
  startDate: string
  endDate: string
  dueTime: string
  weekdaySelection: boolean[]
  dayOfMonth: string
  timezone: string
  active: boolean
}

type DefinitionDraft = {
  title: string
  description: string
  tasks: TaskDraft[]
  schedules: ScheduleDraft[]
}

const props = defineProps<{
  organizationId: string
  establishmentId: string
  serviceArea: ChecklistServiceArea
  requestedDefinitionGroupId?: string | null
}>()

const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'requestHandled'): void
}>()

const DEFAULT_TIMEZONE = 'Europe/Oslo'
const WEEKDAY_LABELS = ['mo', 'tu', 'we', 'th', 'fr', 'sa', 'su']
const WEEKDAY_SUMMARY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const TIMEZONE_OPTIONS = [
  'Europe/Oslo',
  'Europe/Stockholm',
  'Europe/Copenhagen',
  'Europe/Helsinki',
  'Europe/London',
  'Europe/Berlin',
  'UTC',
]

const definitions = ref<ChecklistDefinition[]>([])
const isLoading = ref(false)
const isSaving = ref(false)
const errorMessage = ref<string | null>(null)
const statusMessage = ref<string | null>(null)
const editorMode = ref<'create' | 'edit'>('create')
const editingDefinitionId = ref<string | null>(null)
const isEditorOpen = ref(false)
const isManagerCollapsed = ref(true)
const lastHandledRequestedDefinitionGroupId = ref<string | null>(null)

const form = reactive<DefinitionDraft>({
  title: '',
  description: '',
  tasks: [],
  schedules: [],
})

function createId(prefix: string): string {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`
}

function toIsoDate(value: Date): string {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function createTaskDraft(taskKind: ChecklistTaskKind = 'ACTION'): TaskDraft {
  return {
    id: createId('task'),
    title: '',
    details: '',
    taskKind,
    required: true,
    measurementUnit: '',
    minimumAllowedValue: '',
    maximumAllowedValue: '',
  }
}

function createScheduleDraft(scheduleType: ChecklistScheduleType = 'DAILY'): ScheduleDraft {
  return {
    id: createId('schedule'),
    scheduleType,
    startDate: toIsoDate(new Date()),
    endDate: '',
    dueTime: '09:00',
    weekdaySelection: [true, false, false, false, false, false, false],
    dayOfMonth: '1',
    timezone: DEFAULT_TIMEZONE,
    active: true,
  }
}

function resetForm(): void {
  form.title = ''
  form.description = ''
  form.tasks = [createTaskDraft()]
  form.schedules = [createScheduleDraft()]
}

function weekdayMaskFromSelection(selection: boolean[]): number | null {
  let mask = 0

  selection.forEach((isSelected, index) => {
    if (isSelected) {
      mask |= 1 << index
    }
  })

  return mask === 0 ? null : mask
}

function selectionFromWeekdayMask(mask: number | null): boolean[] {
  return Array.from({ length: 7 }, (_, index) => Boolean(mask && mask & (1 << index)))
}

function toTaskDrafts(definition: ChecklistDefinition): TaskDraft[] {
  return definition.tasks.map((task) => ({
    id: task.id,
    title: task.title,
    details: task.details ?? '',
    taskKind: task.taskKind,
    required: task.required,
    measurementUnit: task.measurementUnit ?? '',
    minimumAllowedValue: task.minimumAllowedValue === null ? '' : String(task.minimumAllowedValue),
    maximumAllowedValue: task.maximumAllowedValue === null ? '' : String(task.maximumAllowedValue),
  }))
}

function toScheduleDrafts(definition: ChecklistDefinition): ScheduleDraft[] {
  return definition.schedules.map((schedule) => ({
    id: schedule.id,
    scheduleType: schedule.scheduleType,
    startDate: schedule.startDate,
    endDate: schedule.endDate ?? '',
    dueTime: schedule.dueTime ?? '',
    weekdaySelection: selectionFromWeekdayMask(schedule.weekdayMask),
    dayOfMonth: schedule.dayOfMonth === null ? '' : String(schedule.dayOfMonth),
    timezone: schedule.timezone,
    active: schedule.active,
  }))
}

function openCreateEditor(): void {
  editorMode.value = 'create'
  editingDefinitionId.value = null
  isManagerCollapsed.value = false
  statusMessage.value = null
  errorMessage.value = null
  resetForm()
  isEditorOpen.value = true
}

function openEditEditor(definition: ChecklistDefinition): void {
  if (
    editorMode.value === 'edit' &&
    isEditorOpen.value &&
    editingDefinitionId.value === definition.id
  ) {
    closeEditor()
    return
  }

  editorMode.value = 'edit'
  editingDefinitionId.value = definition.id
  isManagerCollapsed.value = false
  statusMessage.value = null
  errorMessage.value = null
  form.title = definition.title
  form.description = definition.description ?? ''
  form.tasks = toTaskDrafts(definition)
  form.schedules = toScheduleDrafts(definition)
  isEditorOpen.value = true
}

function closeEditor(): void {
  isEditorOpen.value = false
  editingDefinitionId.value = null
}

function toggleManagerCollapsed(): void {
  isManagerCollapsed.value = !isManagerCollapsed.value
}

function addTask(): void {
  form.tasks.push(createTaskDraft())
}

function removeTask(taskId: string): void {
  if (form.tasks.length === 1) {
    return
  }

  form.tasks = form.tasks.filter((task) => task.id !== taskId)
}

function moveTask(taskId: string, direction: -1 | 1): void {
  const index = form.tasks.findIndex((task) => task.id === taskId)

  if (index < 0) {
    return
  }

  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= form.tasks.length) {
    return
  }

  const updatedTasks = [...form.tasks]
  const [movedTask] = updatedTasks.splice(index, 1)
  if (!movedTask) {
    return
  }
  updatedTasks.splice(nextIndex, 0, movedTask)
  form.tasks = updatedTasks
}

function addSchedule(): void {
  form.schedules.push(createScheduleDraft())
}

function removeSchedule(scheduleId: string): void {
  if (form.schedules.length === 1) {
    return
  }

  form.schedules = form.schedules.filter((schedule) => schedule.id !== scheduleId)
}

function toggleWeekday(schedule: ScheduleDraft, weekdayIndex: number): void {
  schedule.weekdaySelection = schedule.weekdaySelection.map((selected, index) =>
    index === weekdayIndex ? !selected : selected,
  )
}

function normalizeNullableString(value: string): string | null {
  const normalized = value.trim()
  return normalized.length > 0 ? normalized : null
}

function normalizeNullableNumber(value: string): number | null {
  const normalized = value.trim()
  if (!normalized) {
    return null
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function buildTaskInputs(): ChecklistDefinitionTaskInput[] {
  return form.tasks.map((task, index) => ({
    title: task.title.trim(),
    details: normalizeNullableString(task.details),
    taskKind: task.taskKind,
    required: task.required,
    sortOrder: index,
    measurementUnit: normalizeNullableString(task.measurementUnit),
    minimumAllowedValue: normalizeNullableNumber(task.minimumAllowedValue),
    maximumAllowedValue: normalizeNullableNumber(task.maximumAllowedValue),
  }))
}

function buildScheduleInputs(): ChecklistDefinitionScheduleInput[] {
  return form.schedules.map((schedule) => ({
    scheduleType: schedule.scheduleType,
    startDate: schedule.startDate,
    endDate: normalizeNullableString(schedule.endDate),
    dueTime: normalizeNullableString(schedule.dueTime),
    weekdayMask:
      schedule.scheduleType === 'WEEKLY'
        ? weekdayMaskFromSelection(schedule.weekdaySelection)
        : null,
    dayOfMonth:
      schedule.scheduleType === 'MONTHLY' ? normalizeNullableNumber(schedule.dayOfMonth) : null,
    timezone: normalizeNullableString(schedule.timezone) ?? DEFAULT_TIMEZONE,
    active: schedule.active,
  }))
}

function validateDraft(): string | null {
  if (!form.title.trim()) {
    return 'Title is required.'
  }

  if (form.tasks.length === 0) {
    return 'Add at least one task.'
  }

  if (form.tasks.some((task) => !task.title.trim())) {
    return 'Every task needs a title.'
  }

  if (
    form.tasks.some(
      (task) =>
        task.taskKind === 'MEASUREMENT' &&
        !task.measurementUnit.trim() &&
        !task.minimumAllowedValue.trim() &&
        !task.maximumAllowedValue.trim(),
    )
  ) {
    return 'Measurement tasks should include a unit or range.'
  }

  if (form.schedules.length === 0) {
    return 'Add at least one schedule.'
  }

  for (const schedule of form.schedules) {
    if (!schedule.startDate) {
      return 'Every schedule needs a start date.'
    }

    if (
      schedule.scheduleType === 'WEEKLY' &&
      weekdayMaskFromSelection(schedule.weekdaySelection) === null
    ) {
      return 'Pick at least one weekday for weekly schedules.'
    }

    if (
      schedule.scheduleType === 'MONTHLY' &&
      (normalizeNullableNumber(schedule.dayOfMonth) === null ||
        Number(schedule.dayOfMonth) < 1 ||
        Number(schedule.dayOfMonth) > 31)
    ) {
      return 'Monthly schedules need a day between 1 and 31.'
    }
  }

  return null
}

async function loadDefinitions(): Promise<void> {
  isLoading.value = true
  errorMessage.value = null

  try {
    const response = await listChecklistDefinitions({
      organizationId: props.organizationId,
      establishmentId: props.establishmentId,
      serviceArea: props.serviceArea,
      size: 200,
    })

    definitions.value = response.items
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load checklist setups.'
  } finally {
    isLoading.value = false
  }
}

async function submitDefinition(): Promise<void> {
  const validationMessage = validateDraft()
  if (validationMessage) {
    errorMessage.value = validationMessage
    return
  }

  const input: UpsertChecklistDefinitionInput = {
    title: form.title.trim(),
    description: normalizeNullableString(form.description),
    serviceArea: props.serviceArea,
    status: 'ACTIVE',
    tasks: buildTaskInputs(),
    schedules: buildScheduleInputs(),
  }

  isSaving.value = true
  errorMessage.value = null

  try {
    if (editorMode.value === 'create') {
      await createChecklistDefinition(
        {
          organizationId: props.organizationId,
          establishmentId: props.establishmentId,
        },
        input,
      )
      statusMessage.value = 'Checklist setup created.'
    } else if (editingDefinitionId.value) {
      await updateChecklistDefinition(
        {
          organizationId: props.organizationId,
          establishmentId: props.establishmentId,
          checklistDefinitionId: editingDefinitionId.value,
        },
        input,
      )
      statusMessage.value = 'Checklist setup updated.'
    }

    await loadDefinitions()
    emit('saved')
    closeEditor()
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to save checklist setup.'
  } finally {
    isSaving.value = false
  }
}

async function archiveDefinition(definition: ChecklistDefinition): Promise<void> {
  const shouldArchive = window.confirm(
    `Delete "${definition.title}"? This archives the checklist setup and stops future scheduled runs.`,
  )

  if (!shouldArchive) {
    return
  }

  isSaving.value = true
  errorMessage.value = null

  try {
    await updateChecklistDefinition(
      {
        organizationId: props.organizationId,
        establishmentId: props.establishmentId,
        checklistDefinitionId: definition.id,
      },
      {
        title: definition.title,
        description: definition.description,
        serviceArea: definition.serviceArea,
        status: 'ARCHIVED',
        tasks: definition.tasks.map((task) => ({
          title: task.title,
          details: task.details,
          taskKind: task.taskKind,
          required: task.required,
          sortOrder: task.sortOrder,
          measurementUnit: task.measurementUnit,
          minimumAllowedValue: task.minimumAllowedValue,
          maximumAllowedValue: task.maximumAllowedValue,
        })),
        schedules: definition.schedules.map((schedule) => ({
          scheduleType: schedule.scheduleType,
          startDate: schedule.startDate,
          endDate: schedule.endDate,
          dueTime: schedule.dueTime,
          weekdayMask: schedule.weekdayMask,
          dayOfMonth: schedule.dayOfMonth,
          timezone: schedule.timezone,
          active: schedule.active,
        })),
      },
    )

    statusMessage.value = 'Checklist setup archived.'

    if (editingDefinitionId.value === definition.id) {
      closeEditor()
    }

    await loadDefinitions()
    emit('saved')
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to archive checklist setup.'
  } finally {
    isSaving.value = false
  }
}

function scheduleSummary(schedule: ScheduleDraft | ChecklistDefinitionSchedule): string {
  switch (schedule.scheduleType) {
    case 'ONE_OFF':
      return 'One-off'
    case 'DAILY':
      return 'Daily'
    case 'WEEKLY': {
      const weekdayMask =
        'weekdaySelection' in schedule
          ? weekdayMaskFromSelection(schedule.weekdaySelection)
          : schedule.weekdayMask

      const labels = WEEKDAY_SUMMARY_LABELS.filter((_, index) =>
        Boolean(weekdayMask && weekdayMask & (1 << index)),
      )
      return labels.length > 0 ? `Weekly · ${labels.join(', ')}` : 'Weekly'
    }
    case 'MONTHLY':
      return `Monthly · day ${schedule.dayOfMonth ?? '?'}`
  }
}

function maybeOpenRequestedDefinition(): void {
  const definitionGroupId = props.requestedDefinitionGroupId

  if (!definitionGroupId) {
    lastHandledRequestedDefinitionGroupId.value = null
    return
  }

  if (definitionGroupId === lastHandledRequestedDefinitionGroupId.value) {
    return
  }

  const matchingDefinition = definitions.value.find(
    (definition) => definition.definitionGroupId === definitionGroupId,
  )

  if (!matchingDefinition) {
    return
  }

  openEditEditor(matchingDefinition)
  isManagerCollapsed.value = false
  lastHandledRequestedDefinitionGroupId.value = definitionGroupId
  emit('requestHandled')
}

const definitionCountLabel = computed(() => {
  return `${definitions.value.length} ${definitions.value.length === 1 ? 'active setup' : 'active setups'}`
})

const activeDefinition = computed(() => {
  if (!editingDefinitionId.value) {
    return null
  }

  return definitions.value.find((definition) => definition.id === editingDefinitionId.value) ?? null
})

const editorTitle = computed(() => {
  return editorMode.value === 'create' ? 'Create checklist setup' : 'Edit checklist setup'
})

watch(
  () => [props.organizationId, props.establishmentId, props.serviceArea],
  async () => {
    await loadDefinitions()
  },
  { immediate: true },
)

watch(() => props.requestedDefinitionGroupId, maybeOpenRequestedDefinition)
watch(definitions, maybeOpenRequestedDefinition)
</script>

<template>
  <section class="manager-shell">
    <header
      class="manager-header manager-header-toggle"
      role="button"
      tabindex="0"
      :aria-expanded="!isManagerCollapsed"
      aria-label="Toggle checklist setups"
      @click="toggleManagerCollapsed"
      @keydown.enter.prevent="toggleManagerCollapsed"
      @keydown.space.prevent="toggleManagerCollapsed"
    >
      <div class="manager-copy">
        <div class="manager-title-row">
          <component :is="isManagerCollapsed ? ChevronDown : ChevronUp" :size="16" />
          <h2>Checklist setups</h2>
        </div>
        <p>
          Create one-off and recurring setups for this establishment. Active setups generate runs
          automatically.
        </p>
      </div>

      <div class="manager-actions">
        <button type="button" class="btn btn-secondary" @click.stop="openCreateEditor">
          New setup
        </button>
        <button
          v-if="isEditorOpen"
          type="button"
          class="btn btn-secondary"
          @click.stop="closeEditor"
        >
          Close editor
        </button>
      </div>
    </header>

    <p v-if="isManagerCollapsed" class="manager-collapsed-summary">
      {{ definitionCountLabel }}. Expand to manage checklist setups.
    </p>

    <template v-else>
      <p v-if="statusMessage" class="feedback-message feedback-success">{{ statusMessage }}</p>
      <p v-if="errorMessage" class="feedback-message feedback-error">{{ errorMessage }}</p>

      <div v-if="isLoading" class="manager-state-card">
        <p>Loading checklist setups...</p>
      </div>

      <div v-else-if="definitions.length === 0" class="manager-state-card">
        <p>No checklist setups yet. Create the first one-off or recurring setup here.</p>
      </div>

      <div v-else class="definition-list">
        <template v-for="definition in definitions" :key="definition.id">
          <article
            class="definition-row"
            :data-active="editingDefinitionId === definition.id"
            @click="openEditEditor(definition)"
          >
            <div class="definition-row-copy">
              <p class="definition-row-meta">
                {{ definition.tasks.length }} tasks · {{ definition.schedules.length }} schedules
              </p>
              <h3>{{ definition.title }}</h3>
              <p class="definition-row-description">
                {{ definition.description || 'No description added.' }}
              </p>
              <div class="definition-chip-row">
                <span
                  v-for="schedule in definition.schedules"
                  :key="schedule.id"
                  class="definition-chip"
                >
                  {{ scheduleSummary(schedule) }}
                </span>
              </div>
            </div>

            <div class="definition-row-actions">
              <button
                type="button"
                class="btn btn-danger-ghost"
                :disabled="isSaving"
                @click.stop="archiveDefinition(definition)"
              >
                Delete
              </button>
            </div>
          </article>

          <form
            v-if="isEditorOpen && editorMode === 'edit' && editingDefinitionId === definition.id"
            class="editor-form editor-form-submenu"
            @submit.prevent="submitDefinition"
          >
            <section class="editor-panel">
              <header class="editor-header">
                <div>
                  <p class="section-kicker">selected setup submenu</p>
                  <h3>{{ editorTitle }}</h3>
                  <p class="editor-parent-label">
                    Editing tasks and schedule for <strong>{{ definition.title }}</strong>
                  </p>
                </div>
                <p class="editor-summary">{{ definitionCountLabel }}</p>
              </header>

              <div class="form-grid">
                <label class="field-block field-block-wide">
                  <span>Title</span>
                  <input v-model="form.title" type="text" maxlength="255" />
                </label>

                <label class="field-block field-block-wide">
                  <span>Description</span>
                  <textarea v-model="form.description" rows="3" maxlength="2000" />
                </label>
              </div>
            </section>

            <section class="editor-panel">
              <div class="section-header">
                <div>
                  <p class="section-kicker">Checklist tasks</p>
                  <h3>Tasks in this checklist</h3>
                </div>
                <button type="button" class="btn btn-secondary" @click="addTask">Add task</button>
              </div>

              <div class="builder-list">
                <article
                  v-for="(task, taskIndex) in form.tasks"
                  :key="task.id"
                  class="builder-card"
                >
                  <header class="builder-card-header">
                    <strong>Task {{ taskIndex + 1 }}</strong>
                    <div class="builder-card-actions">
                      <button
                        type="button"
                        class="icon-button"
                        aria-label="Move task up"
                        @click="moveTask(task.id, -1)"
                      >
                        <ArrowUp :size="14" />
                      </button>
                      <button
                        type="button"
                        class="icon-button"
                        aria-label="Move task down"
                        @click="moveTask(task.id, 1)"
                      >
                        <ArrowDown :size="14" />
                      </button>
                      <button
                        type="button"
                        class="icon-button icon-button-danger"
                        aria-label="Remove task"
                        @click="removeTask(task.id)"
                      >
                        <Trash2 :size="14" />
                      </button>
                    </div>
                  </header>

                  <div class="form-grid">
                    <label class="field-block">
                      <span>Task title</span>
                      <input v-model="task.title" type="text" maxlength="500" />
                    </label>

                    <label class="field-block">
                      <span>Task kind</span>
                      <select v-model="task.taskKind">
                        <option value="ACTION">Action</option>
                        <option value="VERIFICATION">Verification</option>
                        <option value="MEASUREMENT">Measurement</option>
                        <option value="TEXT_ENTRY">Text entry</option>
                      </select>
                    </label>

                    <label class="field-block field-block-wide">
                      <span>Details</span>
                      <textarea v-model="task.details" rows="3" maxlength="1000" />
                    </label>

                    <label class="checkbox-row">
                      <input v-model="task.required" type="checkbox" />
                      <span>Required task</span>
                    </label>

                    <template v-if="task.taskKind === 'MEASUREMENT'">
                      <label class="field-block">
                        <span>Unit</span>
                        <input
                          v-model="task.measurementUnit"
                          type="text"
                          maxlength="32"
                          placeholder="C"
                        />
                      </label>

                      <label class="field-block">
                        <span>Minimum value</span>
                        <input v-model="task.minimumAllowedValue" type="number" step="0.01" />
                      </label>

                      <label class="field-block">
                        <span>Maximum value</span>
                        <input v-model="task.maximumAllowedValue" type="number" step="0.01" />
                      </label>
                    </template>
                  </div>
                </article>
              </div>
            </section>

            <section class="editor-panel">
              <div class="section-header">
                <div>
                  <p class="section-kicker">Checklist schedule</p>
                  <h3>Schedules in this checklist</h3>
                </div>
                <button type="button" class="btn btn-secondary" @click="addSchedule">
                  Add schedule
                </button>
              </div>

              <div class="builder-list">
                <article
                  v-for="(schedule, scheduleIndex) in form.schedules"
                  :key="schedule.id"
                  class="builder-card"
                >
                  <header class="builder-card-header">
                    <strong>Schedule {{ scheduleIndex + 1 }}</strong>
                    <button
                      type="button"
                      class="icon-button icon-button-danger"
                      aria-label="Remove schedule"
                      @click="removeSchedule(schedule.id)"
                    >
                      <Trash2 :size="14" />
                    </button>
                  </header>

                  <div class="form-grid">
                    <label class="field-block">
                      <span>Pattern</span>
                      <select v-model="schedule.scheduleType">
                        <option value="ONE_OFF">One-off</option>
                        <option value="DAILY">Daily</option>
                        <option value="WEEKLY">Weekly</option>
                        <option value="MONTHLY">Monthly</option>
                      </select>
                    </label>

                    <label class="field-block">
                      <span>Due time</span>
                      <input v-model="schedule.dueTime" type="time" />
                    </label>

                    <label class="field-block">
                      <span>Start date</span>
                      <input v-model="schedule.startDate" type="date" />
                    </label>

                    <label v-if="schedule.scheduleType !== 'ONE_OFF'" class="field-block">
                      <span>End date</span>
                      <input v-model="schedule.endDate" type="date" />
                    </label>

                    <label class="field-block">
                      <span>Timezone</span>
                      <input
                        v-model="schedule.timezone"
                        type="text"
                        maxlength="64"
                        list="checklist-timezone-options"
                        placeholder="Select or type timezone"
                      />
                    </label>

                    <label v-if="schedule.scheduleType === 'MONTHLY'" class="field-block">
                      <span>Day of month</span>
                      <input v-model="schedule.dayOfMonth" type="number" min="1" max="31" />
                    </label>

                    <div
                      v-if="schedule.scheduleType === 'WEEKLY'"
                      class="field-block field-block-wide"
                    >
                      <span>Days</span>
                      <div class="weekday-grid">
                        <button
                          v-for="(label, weekdayIndex) in WEEKDAY_LABELS"
                          :key="label"
                          type="button"
                          class="weekday-square"
                          :data-active="schedule.weekdaySelection[weekdayIndex]"
                          @click="toggleWeekday(schedule, weekdayIndex)"
                        >
                          <span class="weekday-square-box"></span>
                          <span>{{ label }}</span>
                        </button>
                      </div>
                    </div>

                    <label class="checkbox-row">
                      <input v-model="schedule.active" type="checkbox" />
                      <span>Schedule is active</span>
                    </label>
                  </div>
                </article>
              </div>
            </section>

            <footer class="editor-actions">
              <button type="button" class="btn btn-secondary" @click="closeEditor">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="isSaving">
                {{ isSaving ? 'Saving...' : 'Save changes' }}
              </button>
            </footer>
          </form>
        </template>
      </div>

      <form
        v-if="isEditorOpen && editorMode === 'create'"
        class="editor-form"
        @submit.prevent="submitDefinition"
      >
        <section class="editor-panel">
          <header class="editor-header">
            <div>
              <p class="section-kicker">new setup</p>
              <h3>{{ editorTitle }}</h3>
            </div>
            <p class="editor-summary">{{ definitionCountLabel }}</p>
          </header>

          <div class="form-grid">
            <label class="field-block field-block-wide">
              <span>Title</span>
              <input v-model="form.title" type="text" maxlength="255" />
            </label>

            <label class="field-block field-block-wide">
              <span>Description</span>
              <textarea v-model="form.description" rows="3" maxlength="2000" />
            </label>
          </div>
        </section>

        <section class="editor-panel">
          <div class="section-header">
            <div>
              <p class="section-kicker">Checklist tasks</p>
              <h3>Tasks in this checklist</h3>
            </div>
            <button type="button" class="btn btn-secondary" @click="addTask">Add task</button>
          </div>

          <div class="builder-list">
            <article v-for="(task, taskIndex) in form.tasks" :key="task.id" class="builder-card">
              <header class="builder-card-header">
                <strong>Task {{ taskIndex + 1 }}</strong>
                <div class="builder-card-actions">
                  <button
                    type="button"
                    class="icon-button"
                    aria-label="Move task up"
                    @click="moveTask(task.id, -1)"
                  >
                    <ArrowUp :size="14" />
                  </button>
                  <button
                    type="button"
                    class="icon-button"
                    aria-label="Move task down"
                    @click="moveTask(task.id, 1)"
                  >
                    <ArrowDown :size="14" />
                  </button>
                  <button
                    type="button"
                    class="icon-button icon-button-danger"
                    aria-label="Remove task"
                    @click="removeTask(task.id)"
                  >
                    <Trash2 :size="14" />
                  </button>
                </div>
              </header>

              <div class="form-grid">
                <label class="field-block">
                  <span>Task title</span>
                  <input v-model="task.title" type="text" maxlength="500" />
                </label>

                <label class="field-block">
                  <span>Task kind</span>
                  <select v-model="task.taskKind">
                    <option value="ACTION">Action</option>
                    <option value="VERIFICATION">Verification</option>
                    <option value="MEASUREMENT">Measurement</option>
                    <option value="TEXT_ENTRY">Text entry</option>
                  </select>
                </label>

                <label class="field-block field-block-wide">
                  <span>Details</span>
                  <textarea v-model="task.details" rows="3" maxlength="1000" />
                </label>

                <label class="checkbox-row">
                  <input v-model="task.required" type="checkbox" />
                  <span>Required task</span>
                </label>

                <template v-if="task.taskKind === 'MEASUREMENT'">
                  <label class="field-block">
                    <span>Unit</span>
                    <input
                      v-model="task.measurementUnit"
                      type="text"
                      maxlength="32"
                      placeholder="C"
                    />
                  </label>

                  <label class="field-block">
                    <span>Minimum value</span>
                    <input v-model="task.minimumAllowedValue" type="number" step="0.01" />
                  </label>

                  <label class="field-block">
                    <span>Maximum value</span>
                    <input v-model="task.maximumAllowedValue" type="number" step="0.01" />
                  </label>
                </template>
              </div>
            </article>
          </div>
        </section>

        <section class="editor-panel">
          <div class="section-header">
            <div>
              <p class="section-kicker">Checklist schedule</p>
              <h3>Schedules in this checklist</h3>
            </div>
            <button type="button" class="btn btn-secondary" @click="addSchedule">
              Add schedule
            </button>
          </div>

          <div class="builder-list">
            <article
              v-for="(schedule, scheduleIndex) in form.schedules"
              :key="schedule.id"
              class="builder-card"
            >
              <header class="builder-card-header">
                <strong>Schedule {{ scheduleIndex + 1 }}</strong>
                <button
                  type="button"
                  class="icon-button icon-button-danger"
                  aria-label="Remove schedule"
                  @click="removeSchedule(schedule.id)"
                >
                  <Trash2 :size="14" />
                </button>
              </header>

              <div class="form-grid">
                <label class="field-block">
                  <span>Pattern</span>
                  <select v-model="schedule.scheduleType">
                    <option value="ONE_OFF">One-off</option>
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </label>

                <label class="field-block">
                  <span>Due time</span>
                  <input v-model="schedule.dueTime" type="time" />
                </label>

                <label class="field-block">
                  <span>Start date</span>
                  <input v-model="schedule.startDate" type="date" />
                </label>

                <label v-if="schedule.scheduleType !== 'ONE_OFF'" class="field-block">
                  <span>End date</span>
                  <input v-model="schedule.endDate" type="date" />
                </label>

                <label class="field-block">
                  <span>Timezone</span>
                  <input
                    v-model="schedule.timezone"
                    type="text"
                    maxlength="64"
                    list="checklist-timezone-options"
                    placeholder="Select or type timezone"
                  />
                </label>

                <label v-if="schedule.scheduleType === 'MONTHLY'" class="field-block">
                  <span>Day of month</span>
                  <input v-model="schedule.dayOfMonth" type="number" min="1" max="31" />
                </label>

                <div v-if="schedule.scheduleType === 'WEEKLY'" class="field-block field-block-wide">
                  <span>Days</span>
                  <div class="weekday-grid">
                    <button
                      v-for="(label, weekdayIndex) in WEEKDAY_LABELS"
                      :key="label"
                      type="button"
                      class="weekday-square"
                      :data-active="schedule.weekdaySelection[weekdayIndex]"
                      @click="toggleWeekday(schedule, weekdayIndex)"
                    >
                      <span class="weekday-square-box"></span>
                      <span>{{ label }}</span>
                    </button>
                  </div>
                </div>

                <label class="checkbox-row">
                  <input v-model="schedule.active" type="checkbox" />
                  <span>Schedule is active</span>
                </label>
              </div>
            </article>
          </div>
        </section>

        <footer class="editor-actions">
          <button type="button" class="btn btn-secondary" @click="closeEditor">Cancel</button>
          <button type="submit" class="btn btn-primary" :disabled="isSaving">
            {{ isSaving ? 'Saving...' : 'Create setup' }}
          </button>
        </footer>
      </form>
      <datalist id="checklist-timezone-options">
        <option v-for="timezone in TIMEZONE_OPTIONS" :key="timezone" :value="timezone" />
      </datalist>
    </template>
  </section>
</template>

<style scoped>
.manager-shell,
.manager-header,
.manager-actions,
.definition-list,
.definition-row,
.definition-row-actions,
.editor-form,
.editor-panel,
.editor-header,
.section-header,
.builder-list,
.builder-card,
.builder-card-header,
.builder-card-actions,
.editor-actions {
  display: flex;
}

.manager-shell,
.editor-form,
.editor-panel,
.builder-card {
  flex-direction: column;
}

.manager-shell {
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--color-border-muted);
  border-radius: 6px;
  background: color-mix(in srgb, var(--color-container) 96%, #f5f9ff);
}

.manager-header,
.section-header,
.editor-header,
.builder-card-header,
.editor-actions {
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.manager-header-toggle {
  cursor: pointer;
}

.manager-copy h2,
.section-header h3,
.editor-header h3,
.definition-row-copy h3,
.manager-copy p,
.definition-row-meta,
.definition-row-description,
.editor-parent-label,
.editor-summary,
.feedback-message,
.manager-state-card p {
  margin: 0;
}

.manager-copy {
  display: grid;
  gap: 4px;
}

.manager-title-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.manager-copy p,
.definition-row-description,
.editor-parent-label,
.editor-summary,
.manager-state-card p {
  color: var(--color-text-secondary);
}

.manager-actions,
.definition-row-actions,
.builder-card-actions,
.editor-actions {
  gap: 8px;
}

.manager-actions .btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.manager-header-toggle:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.manager-collapsed-summary {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.feedback-message,
.manager-state-card,
.definition-row,
.editor-panel {
  padding: 12px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-container);
}

.feedback-success {
  border-color: #b8e2ca;
  color: var(--color-success);
}

.feedback-error {
  border-color: #f0b8b8;
  color: var(--color-critical);
}

.definition-list,
.builder-list {
  flex-direction: column;
  gap: 8px;
}

.editor-panel:first-child .form-grid {
  gap: 12px;
}

.definition-row {
  gap: 12px;
  justify-content: space-between;
  cursor: pointer;
  transition:
    border-color 120ms ease,
    box-shadow 120ms ease,
    background-color 120ms ease;
}

.definition-row:hover,
.definition-row[data-active='true'] {
  border-color: var(--color-primary);
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.definition-row[data-active='true'] {
  background: color-mix(in srgb, var(--color-container) 90%, #eff6ff);
}

.definition-row-copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.definition-row-meta,
.section-kicker,
.field-block span,
.checkbox-row span {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.definition-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.definition-chip {
  display: inline-flex;
  align-items: center;
  min-height: 1.5rem;
  padding: 0.15rem 0.45rem;
  border: 1px solid #b6d3ff;
  border-radius: 999px;
  background: #eff6ff;
  color: var(--color-primary);
  font-size: 0.6875rem;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.builder-card .form-grid {
  gap: 8px 10px;
}

.editor-form-submenu {
  position: relative;
  gap: 8px;
  margin-left: 18px;
  padding-left: 14px;
}

.editor-form-submenu::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: color-mix(in srgb, var(--color-primary) 40%, white);
}

.editor-form-submenu::after {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  width: 12px;
  height: 2px;
  background: color-mix(in srgb, var(--color-primary) 40%, white);
}

.field-block {
  display: grid;
  gap: 4px;
}

.builder-card .field-block,
.builder-card .checkbox-row {
  gap: 6px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-block input,
.field-block textarea,
.field-block select {
  width: 100%;
  min-height: 34px;
  padding: 0.5rem 0.45rem;
  border: none;
  border-bottom: 1px solid var(--color-border-muted);
  border-radius: 4px 4px 0 0;
  background: color-mix(in srgb, var(--color-container) 90%, white);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.field-block textarea {
  min-height: 64px;
  resize: vertical;
}

.builder-card input,
.builder-card select {
  min-height: 32px;
  padding-top: 0.45rem;
  padding-bottom: 0.45rem;
}

.builder-card textarea {
  min-height: 56px;
  padding-top: 0.45rem;
  padding-bottom: 0.45rem;
}

.field-block input:focus,
.field-block textarea:focus,
.field-block select:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-bottom-color: transparent;
}

.checkbox-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.checkbox-row input {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
}

.builder-card {
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: color-mix(in srgb, var(--color-container) 94%, white);
}

.builder-card-header {
  gap: 8px;
}

.builder-card-header strong {
  font-size: 0.75rem;
}

.builder-card-actions {
  gap: 4px;
}

.editor-panel .builder-card {
  gap: 8px;
  padding: 10px;
}

.editor-form-submenu .editor-panel {
  background: color-mix(in srgb, var(--color-container) 92%, #eff6ff);
}

.btn,
.text-button,
.icon-button,
.weekday-square {
  font: 600 0.75rem var(--font-sans, inherit);
}

.btn {
  min-height: 2rem;
  padding: 0.35rem 0.6rem;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  transition:
    background-color 120ms ease,
    border-color 120ms ease,
    color 120ms ease;
}

.btn:disabled,
.text-button:disabled,
.icon-button:disabled {
  opacity: 0.6;
  pointer-events: none;
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-white);
}

.btn-primary:hover {
  background: color-mix(in srgb, var(--color-primary) 88%, black);
}

.btn-secondary {
  background: transparent;
  color: var(--color-text-primary);
  border-color: var(--color-border-muted);
}

.btn-secondary:hover {
  background: var(--color-surface);
}

.btn-danger-ghost,
.text-button-danger {
  color: var(--color-critical);
}

.btn-danger-ghost {
  background: transparent;
}

.btn-danger-ghost:hover,
.text-button-danger:hover {
  background: #fef2f2;
}

.text-button {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.625rem;
}

.text-button:hover {
  color: var(--color-primary);
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  padding: 0;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.icon-button:hover {
  background: var(--color-surface);
  color: var(--color-primary);
}

.icon-button-danger {
  color: var(--color-critical);
}

.icon-button-danger:hover {
  background: #fef2f2;
  color: var(--color-critical);
}

.weekday-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.weekday-square {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-height: 58px;
  padding: 0.45rem 0.3rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background: var(--color-container);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.weekday-square-box {
  position: relative;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-radius: 3px;
}

.weekday-square[data-active='true'] {
  border-color: #8ab6ff;
  background: #eff6ff;
  color: var(--color-primary);
}

.weekday-square[data-active='true'] .weekday-square-box {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.weekday-square[data-active='true'] .weekday-square-box::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 8px;
  border-right: 2px solid var(--color-white);
  border-bottom: 2px solid var(--color-white);
  transform: translate(-50%, -60%) rotate(45deg);
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .weekday-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .definition-row {
    flex-direction: column;
  }

  .editor-form-submenu {
    margin-left: 12px;
    padding-left: 14px;
  }

  .definition-row-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .manager-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
