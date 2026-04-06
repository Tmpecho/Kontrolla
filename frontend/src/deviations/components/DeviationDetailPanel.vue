<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type {
  DeviationCategory,
  DeviationListItem,
  DeviationMemberOption,
  DeviationSaveInput,
  DeviationSeverity,
  DeviationStatus,
} from '@/deviations/model/deviation.types'
import {
  deviationCategoriesByServiceArea,
  formatDeviationSeverity as formatSeverity,
  formatDeviationStatus as formatStatus,
} from '@/deviations/model/deviation.types'

type DeviationEditDraft = {
  title: string
  category: DeviationCategory
  severity: DeviationSeverity
  status: DeviationStatus
  assignedToUserId: string
  description: string
}

const severityOptions: DeviationSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const statusOptions: DeviationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED']

const props = withDefaults(
  defineProps<{
    deviation: DeviationListItem
    memberOptions: DeviationMemberOption[]
    isSaving?: boolean
    saveErrorMessage?: string | null
    showCloseButton?: boolean
  }>(),
  {
    isSaving: false,
    saveErrorMessage: null,
    showCloseButton: false,
  },
)

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', value: DeviationSaveInput): void
}>()

const isEditing = ref(false)
const draft = ref(createDraft(props.deviation))
const categoryOptions = computed(() => deviationCategoriesByServiceArea[props.deviation.serviceArea])
const assigneeOptions = computed(() => {
  const options = [...props.memberOptions]

  if (
    props.deviation.assignedToUserId &&
    !options.some((member) => member.userId === props.deviation.assignedToUserId)
  ) {
    options.unshift({
      userId: props.deviation.assignedToUserId,
      displayName: props.deviation.assignedTo[0] ?? 'Assigned user',
    })
  }

  return options
})

const canSave = computed(() => {
  return draft.value.title.trim().length > 0 && draft.value.description.trim().length > 0
})

const hasChanges = computed(() => {
  return (
    draft.value.title.trim() !== props.deviation.title ||
    draft.value.category !== props.deviation.category ||
    draft.value.severity !== props.deviation.severity ||
    draft.value.status !== props.deviation.status ||
    (draft.value.assignedToUserId || null) !== props.deviation.assignedToUserId ||
    draft.value.description.trim() !== props.deviation.description
  )
})

watch(
  () => props.deviation,
  (deviation) => {
    draft.value = createDraft(deviation)
    isEditing.value = false
  },
  { immediate: true },
)

function createDraft(deviation: DeviationListItem): DeviationEditDraft {
  return {
    title: deviation.title,
    category: deviation.category,
    severity: deviation.severity,
    status: deviation.status,
    assignedToUserId: deviation.assignedToUserId ?? '',
    description: deviation.description,
  }
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatTimelineMoment(value: string) {
  const entryDate = new Date(value)
  const now = new Date()

  const isSameDay =
    entryDate.getFullYear() === now.getFullYear() &&
    entryDate.getMonth() === now.getMonth() &&
    entryDate.getDate() === now.getDate()

  const dateLabel = isSameDay
    ? 'Today'
    : new Intl.DateTimeFormat('nb-NO', {
        day: 'numeric',
        month: 'short',
      }).format(entryDate)

  const timeLabel = new Intl.DateTimeFormat('nb-NO', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(entryDate)

  return `${dateLabel}, ${timeLabel}`.toUpperCase()
}

function getSortedTimelineEntries(deviation: DeviationListItem) {
  return [...deviation.timeline].sort(
    (left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime(),
  )
}

function startEditing() {
  draft.value = createDraft(props.deviation)
  isEditing.value = true
}

function cancelEditing() {
  draft.value = createDraft(props.deviation)
  isEditing.value = false
}

function saveChanges() {
  if (!canSave.value || !hasChanges.value || props.isSaving) {
    return
  }

  emit('save', {
    title: draft.value.title.trim(),
    category: draft.value.category,
    severity: draft.value.severity,
    status: draft.value.status,
    assignedToUserId: draft.value.assignedToUserId || null,
    description: draft.value.description.trim(),
  })
}

function markAsResolved() {
  if (props.deviation.status === 'RESOLVED' || props.isSaving) {
    return
  }

  emit('save', {
    title: props.deviation.title,
    category: props.deviation.category,
    severity: props.deviation.severity,
    status: 'RESOLVED',
    assignedToUserId: props.deviation.assignedToUserId,
    description: props.deviation.description,
  })
}
</script>

<template>
  <section class="detail-panel" aria-label="Selected deviation details">
    <header class="detail-header">
      <div class="detail-header-main">
        <div class="detail-header-copy">
          <h2>{{ isEditing ? 'Update deviation' : deviation.title }}</h2>
          <p class="detail-subtitle">
            {{
              isEditing
                ? 'Edit the deviation details and save the updated record.'
                : 'Corrective follow-up and issue context.'
            }}
          </p>
        </div>

        <div class="detail-header-actions">
          <button
            v-if="!isEditing"
            type="button"
            class="action-button action-button-secondary action-button-compact"
            :disabled="deviation.status === 'RESOLVED' || isSaving"
            @click="markAsResolved"
          >
            Mark as resolved
          </button>
          <button
            v-if="!isEditing"
            type="button"
            class="action-button action-button-primary action-button-compact"
            :disabled="isSaving"
            @click="startEditing"
          >
            Update
          </button>
          <template v-else>
            <button
              type="button"
              class="action-button action-button-secondary"
              :disabled="isSaving"
              @click="cancelEditing"
            >
              Cancel
            </button>
            <button
              type="button"
              class="action-button action-button-primary"
              :disabled="!canSave || !hasChanges || isSaving"
              @click="saveChanges"
            >
              {{ isSaving ? 'Saving...' : 'Save changes' }}
            </button>
          </template>
        </div>
      </div>

      <div class="detail-header-utility">
        <button
          v-if="showCloseButton"
          type="button"
          class="close-button"
          aria-label="Close deviation details"
          @click="$emit('close')"
        >
          <svg aria-hidden="true" class="close-button-icon" viewBox="0 0 20 20">
            <path
              d="M5 5l10 10"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
            />
            <path
              d="M15 5L5 15"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
            />
          </svg>
        </button>
      </div>
    </header>

    <p v-if="saveErrorMessage" class="field-helper">{{ saveErrorMessage }}</p>

    <section v-if="isEditing" class="detail-section">
      <h3>Deviation details</h3>

      <fieldset class="edit-fieldset">
        <div class="edit-field edit-field--full">
          <label class="action-label" for="deviation-title">Title</label>
          <input id="deviation-title" v-model="draft.title" class="action-input" type="text" />
        </div>

        <div class="edit-field">
          <label class="action-label" for="deviation-category">Category</label>
          <select id="deviation-category" v-model="draft.category" class="action-input">
            <option v-for="category in categoryOptions" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
        </div>

        <div class="edit-field">
          <label class="action-label" for="deviation-severity">Severity</label>
          <select id="deviation-severity" v-model="draft.severity" class="action-input">
            <option v-for="severity in severityOptions" :key="severity" :value="severity">
              {{ formatSeverity(severity) }}
            </option>
          </select>
        </div>

        <div class="edit-field">
          <label class="action-label" for="deviation-status">Status</label>
          <select id="deviation-status" v-model="draft.status" class="action-input">
            <option v-for="status in statusOptions" :key="status" :value="status">
              {{ formatStatus(status) }}
            </option>
          </select>
        </div>

        <div class="edit-field edit-field--full">
          <label class="action-label" for="deviation-assignee">Assignee</label>
          <select id="deviation-assignee" v-model="draft.assignedToUserId" class="action-input">
            <option v-if="!deviation.assignedToUserId" value="">Unassigned</option>
            <option v-for="member in assigneeOptions" :key="member.userId" :value="member.userId">
              {{ member.displayName }}
            </option>
          </select>
          <p class="field-helper">A deviation can currently be assigned to one user.</p>
        </div>

        <div class="edit-field edit-field--full">
          <label class="action-label" for="deviation-description">Description</label>
          <textarea
            id="deviation-description"
            v-model="draft.description"
            class="action-input action-textarea"
          />
        </div>
      </fieldset>
    </section>

    <template v-else>
      <dl class="detail-metadata">
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
            <span class="deviation-tag" :data-tone="formatSeverity(deviation.severity)">
              {{ formatSeverity(deviation.severity) }}
            </span>
          </dd>
        </div>
        <div class="metadata-item">
          <dt>Status</dt>
          <dd>
            <span class="deviation-tag" :data-tone="formatStatus(deviation.status)">
              {{ formatStatus(deviation.status) }}
            </span>
          </dd>
        </div>
      </dl>

      <section class="detail-section">
        <h3>Assigned to</h3>
        <div v-if="deviation.assignedTo.length > 0" class="assignee-list">
          <span v-for="assignee in deviation.assignedTo" :key="assignee" class="assignee-chip">
            {{ assignee }}
          </span>
        </div>
        <p v-else class="field-helper">Not assigned yet.</p>
      </section>

      <section class="detail-section">
        <h3>Description</h3>
        <p class="detail-body">{{ deviation.description }}</p>
      </section>
    </template>

    <section class="detail-section">
      <h3>Corrective timeline</h3>
      <ol class="timeline-list">
        <li
          v-for="timelineEntry in getSortedTimelineEntries(deviation)"
          :key="timelineEntry.id"
          class="timeline-entry"
        >
          <div class="timeline-marker" aria-hidden="true">
            <span class="timeline-dot"></span>
            <span class="timeline-line"></span>
          </div>

          <div class="timeline-entry-content">
            <p class="timeline-date">{{ formatTimelineMoment(timelineEntry.createdAt) }}</p>
            <p class="timeline-note">{{ timelineEntry.note }}</p>
            <p class="timeline-author">Added by {{ timelineEntry.authorName }}</p>
          </div>
        </li>
      </ol>
      <p v-if="deviation.timeline.length === 0" class="field-helper">
        No corrective activity has been recorded yet.
      </p>
    </section>
  </section>
</template>

<style scoped>
.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-header-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
}

.detail-header-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
}

.detail-header-utility {
  display: flex;
  flex-shrink: 0;
  align-items: flex-start;
}

.detail-header-copy h2,
.detail-subtitle,
.metadata-item dt,
.metadata-item dd,
.detail-section h3,
.detail-body,
.field-helper,
.timeline-date,
.timeline-note,
.timeline-author {
  margin: 0;
}

.detail-subtitle,
.field-helper,
.timeline-date {
  color: var(--color-text-secondary);
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-primary);
  font: inherit;
  cursor: pointer;
}

.close-button-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.detail-metadata {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metadata-item dt,
.action-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.assignee-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.assignee-chip,
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
}

.deviation-tag {
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

.detail-body {
  color: var(--color-text-primary);
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline-entry {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 0 0 20px;
}

.timeline-marker {
  display: flex;
  width: 18px;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-container);
}

.timeline-line {
  width: 1px;
  flex: 1;
  min-height: 24px;
  margin-top: 6px;
  background-color: var(--color-border-muted);
}

.timeline-entry:last-child {
  padding-bottom: 0;
}

.timeline-entry:last-child .timeline-line {
  display: none;
}

.timeline-entry-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.timeline-date {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.timeline-note {
  color: var(--color-text-primary);
  font-weight: 600;
}

.timeline-author {
  color: var(--color-text-secondary);
}

.edit-fieldset {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  border: 0;
}

.edit-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.edit-field--full {
  grid-column: 1 / -1;
}

.action-input {
  width: 100%;
  padding: 0.875rem 0.75rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-white);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.action-textarea {
  min-height: 120px;
  resize: vertical;
}

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 116px;
  padding: 0.875rem 1rem;
  border-radius: 4px;
  font: inherit;
  font-size: 0.875rem;
  cursor: pointer;
}

.action-button-compact {
  min-width: 0;
  padding: 0.75rem 0.875rem;
}

.action-button:disabled {
  cursor: default;
  opacity: 0.55;
}

.action-button-primary {
  border: 0;
  background-color: var(--color-primary);
  color: var(--color-white);
}

.action-button-secondary {
  border: 1px solid var(--color-border-muted);
  background-color: var(--color-container);
  color: var(--color-text-primary);
}

@media (max-width: 720px) {
  .detail-metadata,
  .edit-fieldset {
    grid-template-columns: 1fr;
  }

  .detail-header {
    flex-direction: column;
  }

  .detail-header-main,
  .detail-header-utility {
    width: 100%;
  }

  .detail-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
