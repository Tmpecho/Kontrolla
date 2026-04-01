<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

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

const filterOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'RECENT', label: 'Recent' },
] as const

const deviationsByService: Record<DeviationServiceArea, DeviationListItem[]> = {
  IK_MAT: [
    {
      id: 'ik-mat-1',
      serviceArea: 'IK_MAT',
      title: 'Raw salmon delivery measured above receiving limit',
      reportedAt: '2026-04-01T09:10:00+02:00',
      category: 'Temperature',
      severity: 'CRITICAL',
      status: 'OPEN',
      assignedTo: ['Nora Johansen', 'Elias Berg'],
      description:
        'The delivery temperature for raw salmon was measured above the internal receiving threshold and requires follow-up with the supplier and kitchen lead.',
      timeline: [
        {
          id: 'ik-mat-1-entry-1',
          createdAt: '2026-04-01T09:18:00+02:00',
          authorName: 'Nora Johansen',
          note: 'Delivery isolated in cold storage pending manager review.',
        },
      ],
    },
    {
      id: 'ik-mat-2',
      serviceArea: 'IK_MAT',
      title: 'Allergen labels missing on prepared takeaway sauces',
      reportedAt: '2026-03-31T16:25:00+02:00',
      category: 'Allergen handling',
      severity: 'HIGH',
      status: 'IN_PROGRESS',
      assignedTo: ['Emil Hansen'],
      description:
        'Prepared takeaway sauces were placed in the front fridge without updated allergen labels after relabeling.',
      timeline: [
        {
          id: 'ik-mat-2-entry-1',
          createdAt: '2026-03-31T16:35:00+02:00',
          authorName: 'Emil Hansen',
          note: 'Affected containers removed from display and sent back to prep.',
        },
      ],
    },
    {
      id: 'ik-mat-3',
      serviceArea: 'IK_MAT',
      title: 'Hand-wash station by prep bench was out of soap',
      reportedAt: '2026-03-31T08:05:00+02:00',
      category: 'Cleaning and hygiene',
      severity: 'MEDIUM',
      status: 'RESOLVED',
      assignedTo: ['Mina Solberg'],
      description:
        'Soap dispenser at the cold-prep sink was empty during the opening check and was replenished after reporting.',
      timeline: [
        {
          id: 'ik-mat-3-entry-1',
          createdAt: '2026-03-31T08:12:00+02:00',
          authorName: 'Mina Solberg',
          note: 'Dispenser refilled and opening stock added to morning checklist notes.',
        },
      ],
    },
    {
      id: 'ik-mat-4',
      serviceArea: 'IK_MAT',
      title: 'Dry storage container missing product date marking',
      reportedAt: '2026-03-30T13:40:00+02:00',
      category: 'Storage and labeling',
      severity: 'LOW',
      status: 'OPEN',
      assignedTo: ['Sander Vik'],
      description:
        'One dry storage container with prepared topping mix did not have an updated product date or batch marking.',
      timeline: [
        {
          id: 'ik-mat-4-entry-1',
          createdAt: '2026-03-30T13:52:00+02:00',
          authorName: 'Sander Vik',
          note: 'Container moved aside for verification of prep batch.',
        },
      ],
    },
  ],
  IK_ALKOHOL: [
    {
      id: 'ik-alkohol-1',
      serviceArea: 'IK_ALKOHOL',
      title: 'Age verification was skipped during late bar service',
      reportedAt: '2026-04-01T00:18:00+02:00',
      category: 'Age control',
      severity: 'HIGH',
      status: 'OPEN',
      assignedTo: ['Lina Dahl', 'Jonas Olsen'],
      description:
        'A guest was served before ID verification was completed during a high-traffic period at the bar.',
      timeline: [
        {
          id: 'ik-alkohol-1-entry-1',
          createdAt: '2026-04-01T00:32:00+02:00',
          authorName: 'Lina Dahl',
          note: 'Shift lead informed and CCTV timestamp recorded for review.',
        },
      ],
    },
    {
      id: 'ik-alkohol-2',
      serviceArea: 'IK_ALKOHOL',
      title: 'Refusal incident log was not completed before shift close',
      reportedAt: '2026-03-31T23:10:00+02:00',
      category: 'Documentation and training',
      severity: 'MEDIUM',
      status: 'IN_PROGRESS',
      assignedTo: ['Amalie Nilsen'],
      description:
        'A refusal of service was handled correctly, but the incident note was not entered before the end of the shift.',
      timeline: [
        {
          id: 'ik-alkohol-2-entry-1',
          createdAt: '2026-03-31T23:22:00+02:00',
          authorName: 'Amalie Nilsen',
          note: 'Manager asked for a full written note before the next evening shift.',
        },
      ],
    },
    {
      id: 'ik-alkohol-3',
      serviceArea: 'IK_ALKOHOL',
      title: 'Guests remained in the serving area past licensed hours',
      reportedAt: '2026-03-30T02:18:00+02:00',
      category: 'Serving hours',
      severity: 'CRITICAL',
      status: 'OPEN',
      assignedTo: ['Henrik Moe'],
      description:
        'Closing routines started too late and several guests remained in the serving area after licensed hours.',
      timeline: [
        {
          id: 'ik-alkohol-3-entry-1',
          createdAt: '2026-03-30T02:35:00+02:00',
          authorName: 'Henrik Moe',
          note: 'Closing sequence is being reviewed with the weekend team.',
        },
      ],
    },
    {
      id: 'ik-alkohol-4',
      serviceArea: 'IK_ALKOHOL',
      title: 'Door host briefing on intoxication handling was missed',
      reportedAt: '2026-03-29T18:45:00+02:00',
      category: 'Intoxicated guest',
      severity: 'LOW',
      status: 'RESOLVED',
      assignedTo: ['Maja Berg'],
      description:
        'The incoming door host was not briefed on the shift-specific intoxication escalation routine during handover.',
      timeline: [
        {
          id: 'ik-alkohol-4-entry-1',
          createdAt: '2026-03-29T19:05:00+02:00',
          authorName: 'Maja Berg',
          note: 'Briefing checklist updated and reviewed with all hosts.',
        },
      ],
    },
  ],
}

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

const filteredDeviations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  let items = deviationsByService[currentServiceArea.value].filter((deviation) => {
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

  items = [...items].sort(
    (left, right) => new Date(right.reportedAt).getTime() - new Date(left.reportedAt).getTime(),
  )

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
</script>

<template>
  <div class="deviation-page">
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
        <li v-for="deviation in filteredDeviations" :key="deviation.id" class="deviation-list-item">
          <article class="deviation-row">
            <div class="deviation-row-header">
              <div>
                <h2>{{ deviation.title }}</h2>
                <p class="deviation-row-hint">
                  Detail view and corrective timeline will be added next.
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
</template>

<style scoped>
.deviation-page {
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
  min-width: 160px;
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
  flex-direction: column;
  gap: 16px;
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
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
</style>
