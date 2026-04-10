<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { RouteLocationRaw } from 'vue-router'

import { useAuthStore } from '@/auth/model/auth.store'
import { listServingHours } from '@/establishments/api/serving-hours.api'
import type { ServingHoursDay } from '@/establishments/model/serving-hours.types'
import { ApiError } from '@/shared/api/http'
import { appEnv } from '@/shared/config/env'

defineProps<{
  dashboardTo: RouteLocationRaw
  editTo: RouteLocationRaw
}>()

const DAY_OF_WEEK_BY_JS_INDEX: ServingHoursDay['dayOfWeek'][] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]

const authStore = useAuthStore()
const servingHours = ref<ServingHoursDay[]>([])
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)
const now = ref(new Date())
let refreshTimer: number | undefined
let requestSequence = 0

const organizationId = computed(
  () => authStore.appContext?.organizationId ?? appEnv.defaultOrganizationId ?? null,
)
const establishmentId = computed(() => {
  if (authStore.appContext?.organizationId) {
    return authStore.appContext.establishmentId ?? null
  }

  return appEnv.defaultEstablishmentId ?? null
})

const canManageServingHours = computed(() => {
  if (authStore.user?.globalRoles.includes('PLATFORM_ADMIN')) {
    return true
  }

  return (
    authStore.appContext?.organizationRole === 'ORG_OWNER' ||
    authStore.appContext?.organizationRole === 'ORG_ADMIN' ||
    authStore.appContext?.organizationRole === 'ORG_MANAGER'
  )
})

const missingContextMessage = computed(() => {
  if (organizationId.value && establishmentId.value) {
    return null
  }

  if (authStore.requiresEstablishmentSelection) {
    return 'Choose an establishment to load serving hours.'
  }

  if (!appEnv.isDevelopment) {
    return 'Serving hours are unavailable until organization context is ready.'
  }

  return 'Set the default organization and establishment IDs or sign in with an organization context to load serving hours.'
})

const servingHoursByDay = computed(() => {
  return new Map(servingHours.value.map((day) => [day.dayOfWeek, day]))
})

const todaySchedule = computed(() => getScheduleForDate(now.value))

const activeSchedule = computed(() => {
  const currentDate = now.value
  const todayInterval = getScheduleInterval(currentDate, 0)
  const yesterdayInterval = getScheduleInterval(currentDate, -1)

  if (todayInterval && isWithinInterval(currentDate, todayInterval)) {
    return todayInterval.schedule
  }

  if (yesterdayInterval && isWithinInterval(currentDate, yesterdayInterval)) {
    return yesterdayInterval.schedule
  }

  return null
})

const isActiveNow = computed(() => Boolean(activeSchedule.value))

const dateLabel = computed(() => {
  const weekday = new Intl.DateTimeFormat('en-GB', { weekday: 'long' })
    .format(now.value)
    .toUpperCase()
  const day = new Intl.DateTimeFormat('en-GB', { day: 'numeric' }).format(now.value)
  const month = new Intl.DateTimeFormat('en-GB', { month: 'short' }).format(now.value).toUpperCase()

  return `TODAY: ${weekday} ${day} ${month}`
})

const currentHoursLabel = computed(() => {
  if (activeSchedule.value) {
    return formatScheduleLabel(activeSchedule.value)
  }

  if (todaySchedule.value) {
    return formatScheduleLabel(todaySchedule.value)
  }

  return 'Closed today'
})

const statusLabel = computed(() => (isActiveNow.value ? 'Active now' : 'Not active now'))

const statusMessage = computed(() => {
  if (activeSchedule.value) {
    return `Serving permitted until ${formatTime(activeSchedule.value.closesAt)}.`
  }

  const nextOpening = getNextOpening(now.value)
  if (!nextOpening) {
    return 'No serving hours are currently configured.'
  }

  return `Next serving starts ${nextOpening.dayLabel} at ${formatTime(nextOpening.schedule.opensAt)}.`
})

function formatScheduleLabel(schedule: ServingHoursDay) {
  return `${formatTime(schedule.opensAt)} - ${formatTime(schedule.closesAt)}`
}

function formatTime(value: string | null) {
  return value ? value.slice(0, 5) : '--:--'
}

function parseTimeToMinutes(value: string | null) {
  if (!value) {
    return null
  }

  const [hours = '0', minutes = '0'] = value.split(':')
  return Number(hours) * 60 + Number(minutes)
}

function getScheduleForDate(date: Date) {
  const dayOfWeek = DAY_OF_WEEK_BY_JS_INDEX[date.getDay()] ?? 'SUNDAY'
  const schedule = servingHoursByDay.value.get(dayOfWeek)

  if (!schedule || schedule.closed || !schedule.opensAt || !schedule.closesAt) {
    return null
  }

  return schedule
}

function getScheduleInterval(referenceDate: Date, dayOffset: number) {
  const intervalDate = new Date(referenceDate)
  intervalDate.setHours(0, 0, 0, 0)
  intervalDate.setDate(intervalDate.getDate() + dayOffset)

  const schedule = getScheduleForDate(intervalDate)
  if (!schedule) {
    return null
  }

  const startMinutes = parseTimeToMinutes(schedule.opensAt)
  const endMinutes = parseTimeToMinutes(schedule.closesAt)
  if (startMinutes === null || endMinutes === null) {
    return null
  }

  const startDate = new Date(intervalDate)
  startDate.setHours(Math.floor(startMinutes / 60), startMinutes % 60, 0, 0)

  const endDate = new Date(intervalDate)
  endDate.setHours(Math.floor(endMinutes / 60), endMinutes % 60, 0, 0)

  if (endMinutes <= startMinutes) {
    endDate.setDate(endDate.getDate() + 1)
  }

  return {
    schedule,
    startDate,
    endDate,
  }
}

function isWithinInterval(value: Date, interval: { startDate: Date; endDate: Date }) {
  return value >= interval.startDate && value < interval.endDate
}

function getNextOpening(referenceDate: Date) {
  for (let offset = 0; offset < 7; offset += 1) {
    const interval = getScheduleInterval(referenceDate, offset)
    if (!interval) {
      continue
    }

    if (referenceDate < interval.startDate) {
      return {
        dayLabel:
          offset === 0
            ? 'today'
            : new Intl.DateTimeFormat('en-GB', { weekday: 'long' }).format(interval.startDate),
        schedule: interval.schedule,
      }
    }
  }

  return null
}

async function loadServingHours() {
  const resolvedOrganizationId = organizationId.value
  const resolvedEstablishmentId = establishmentId.value
  const currentRequestId = ++requestSequence

  if (!resolvedOrganizationId || !resolvedEstablishmentId) {
    servingHours.value = []
    errorMessage.value = null
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = null

  try {
    const response = await listServingHours({
      organizationId: resolvedOrganizationId,
      establishmentId: resolvedEstablishmentId,
    })

    if (currentRequestId !== requestSequence) {
      return
    }

    servingHours.value = response
  } catch (error) {
    if (currentRequestId !== requestSequence) {
      return
    }

    servingHours.value = []
    errorMessage.value =
      error instanceof ApiError ? error.message : 'Failed to load serving hours.'
  } finally {
    if (currentRequestId === requestSequence) {
      isLoading.value = false
    }
  }
}

watch([organizationId, establishmentId], () => {
  void loadServingHours()
}, { immediate: true })

onMounted(() => {
  refreshTimer = window.setInterval(() => {
    now.value = new Date()
  }, 60_000)
})

onBeforeUnmount(() => {
  if (refreshTimer !== undefined) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="serving-hours-tile">
    <div class="tile-header">
      <div class="tile-heading">
        <div>
          <h2>Serving hours</h2>
          <p class="tile-date">{{ dateLabel }}</p>
        </div>
      </div>

      <div class="tile-actions">
        <RouterLink v-if="canManageServingHours" :to="editTo" class="tile-manage-link">
          Edit
        </RouterLink>
        <span :data-state="isActiveNow ? 'active' : 'inactive'" class="status-indicator">
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <p v-if="missingContextMessage" class="tile-hint">{{ missingContextMessage }}</p>
    <p v-else-if="isLoading" class="tile-hint">Loading serving hours...</p>
    <p v-else-if="errorMessage" class="tile-hint">{{ errorMessage }}</p>
    <div v-else class="serving-hours-body">
      <div class="tile-body">
        <p class="hours-range">{{ currentHoursLabel }}</p>
        <p class="tile-hint">{{ statusMessage }}</p>
      </div>
      <RouterLink :to="dashboardTo" aria-hidden="true" class="icon-shell" tabindex="-1">
        <svg class="clock-icon" viewBox="0 0 20 20">
          <circle
            cx="10"
            cy="10"
            fill="none"
            r="7"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.75"
          />
          <path
            d="M10 6.5v4l2.75 1.75"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.75"
          />
        </svg>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.serving-hours-tile,
.serving-hours-tile.dashboard-tile {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 0;
  gap: 18px;
}

.tile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.tile-heading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.tile-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tile-manage-link {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
  text-decoration: none;
}

.icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgb(255 255 255 / 0.92), rgb(255 255 255 / 0.58)),
    linear-gradient(135deg, rgb(23 37 84 / 0.12), rgb(37 99 235 / 0.24));
  color: rgb(29 78 216);
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 0.72),
    0 12px 24px rgb(37 99 235 / 0.12);
}

.clock-icon {
  width: 28px;
  height: 28px;
}

.tile-header h2,
.tile-date,
.hours-range,
.tile-hint {
  margin: 0;
}

.tile-date,
.tile-hint {
  color: var(--color-text-secondary);
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-indicator[data-state='active'] {
  background: color-mix(in srgb, var(--color-success) 16%, white);
  color: var(--color-success);
}

.status-indicator[data-state='inactive'] {
  background: color-mix(in srgb, var(--color-warning) 16%, white);
  color: var(--color-warning);
}

.serving-hours-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.tile-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hours-range {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

@media (max-width: 640px) {
  .tile-header,
  .serving-hours-body {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
