<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type DailyServingHours = {
  start: string
  end: string
}

type WeeklyServingHours = [
  DailyServingHours,
  DailyServingHours,
  DailyServingHours,
  DailyServingHours,
  DailyServingHours,
  DailyServingHours,
  DailyServingHours,
]

const weeklyServingHours: WeeklyServingHours = [
  { start: '13:00', end: '22:00' },
  { start: '13:00', end: '23:00' },
  { start: '13:00', end: '23:00' },
  { start: '13:00', end: '23:00' },
  { start: '13:00', end: '00:30' },
  { start: '13:00', end: '02:00' },
  { start: '12:00', end: '02:00' },
]

const now = ref(new Date())
let refreshTimer: number | undefined

const todaySchedule = computed(() => getScheduleForDay(now.value.getDay()))

const activeSchedule = computed(() => {
  const currentDate = now.value
  const todayInterval = getScheduleInterval(currentDate, 0)
  const yesterdayInterval = getScheduleInterval(currentDate, -1)

  if (isWithinInterval(currentDate, todayInterval)) {
    return todayInterval.schedule
  }

  if (isWithinInterval(currentDate, yesterdayInterval)) {
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
  const schedule = activeSchedule.value ?? todaySchedule.value
  return `${schedule.start} - ${schedule.end}`
})

const statusLabel = computed(() => (isActiveNow.value ? 'Active now' : 'Not active now'))

const statusMessage = computed(() => {
  if (activeSchedule.value) {
    return `Serving permitted until ${activeSchedule.value.end}.`
  }

  const nextOpening = getNextOpening(now.value)
  return `Next serving starts ${nextOpening.dayLabel} at ${nextOpening.time}.`
})

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

function parseTimeToMinutes(value: string) {
  const [hours = '0', minutes = '0'] = value.split(':')
  return Number(hours) * 60 + Number(minutes)
}

function getScheduleForDay(dayIndex: number) {
  switch (dayIndex) {
    case 0:
      return weeklyServingHours[0]
    case 1:
      return weeklyServingHours[1]
    case 2:
      return weeklyServingHours[2]
    case 3:
      return weeklyServingHours[3]
    case 4:
      return weeklyServingHours[4]
    case 5:
      return weeklyServingHours[5]
    case 6:
      return weeklyServingHours[6]
    default:
      return weeklyServingHours[0]
  }
}

function getScheduleInterval(referenceDate: Date, dayOffset: number) {
  const intervalDate = new Date(referenceDate)
  intervalDate.setHours(0, 0, 0, 0)
  intervalDate.setDate(intervalDate.getDate() + dayOffset)

  const schedule = getScheduleForDay(intervalDate.getDay())
  const startMinutes = parseTimeToMinutes(schedule.start)
  const endMinutes = parseTimeToMinutes(schedule.end)

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

    if (referenceDate < interval.startDate) {
      return {
        dayLabel:
          offset === 0
            ? 'today'
            : new Intl.DateTimeFormat('en-GB', { weekday: 'long' }).format(interval.startDate),
        time: interval.schedule.start,
      }
    }
  }

  return {
    dayLabel: 'tomorrow',
    time: todaySchedule.value.start,
  }
}
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

      <span :data-state="isActiveNow ? 'active' : 'inactive'" class="status-indicator">
        {{ statusLabel }}
      </span>
    </div>

    <div class="serving-hours-body">
      <div class="tile-body">
        <p class="hours-range">{{ currentHoursLabel }}</p>
        <p class="tile-hint">{{ statusMessage }}</p>
      </div>
      <div aria-hidden="true" class="icon-shell">
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
      </div>
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

.icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-primary);
}

.clock-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.tile-heading h2,
.tile-date,
.hours-range,
.tile-hint {
  margin: 0;
}

.tile-date {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
}

.status-indicator[data-state='active'] {
  color: var(--color-success);
}

.tile-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.serving-hours-body {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-content: center;
  gap: 16px
}

.hours-range {
  color: var(--color-text-primary);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.tile-hint {
  color: var(--color-text-secondary);
}

@media (max-width: 720px) {
  .tile-header {
    flex-direction: column;
  }
}
</style>
