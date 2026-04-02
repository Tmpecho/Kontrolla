<script setup lang="ts">
import { computed } from 'vue'

import type {
  TemperatureLogEntry,
  TemperatureStatus,
} from '@/ik-mat/model/temperature.types'

const props = defineProps<{
  logs: TemperatureLogEntry[]
  status: TemperatureStatus
}>()

const viewBoxWidth = 100
const viewBoxHeight = 32
const pointRadius = 2.5
const horizontalPadding = pointRadius + 1

const orderedLogs = computed(() => {
  return [...props.logs]
    .sort((left, right) => new Date(left.measuredAt).getTime() - new Date(right.measuredAt).getTime())
    .slice(-7)
})

const points = computed(() => {
  if (orderedLogs.value.length === 0) {
    return ''
  }

  const temperatures = orderedLogs.value.map((logEntry) => logEntry.temperatureCelsius)
  const minimumTemperature = Math.min(...temperatures)
  const maximumTemperature = Math.max(...temperatures)
  const temperatureSpan = maximumTemperature - minimumTemperature || 1

  return orderedLogs.value
    .map((logEntry, index) => {
      const x =
        orderedLogs.value.length === 1
          ? viewBoxWidth / 2
          : horizontalPadding +
            (index / (orderedLogs.value.length - 1)) * (viewBoxWidth - horizontalPadding * 2)
      const normalizedY =
        (logEntry.temperatureCelsius - minimumTemperature) / temperatureSpan
      const y = viewBoxHeight - normalizedY * (viewBoxHeight - 6) - 3

      return `${x},${y}`
    })
    .join(' ')
})

const lastPoint = computed(() => {
  if (!points.value) {
    return null
  }

  const pointCandidates = points.value.split(' ')
  const point = pointCandidates[pointCandidates.length - 1]

  if (!point) {
    return null
  }

  const [x, y] = point.split(',').map(Number)

  if (typeof x !== 'number' || typeof y !== 'number') {
    return null
  }

  return { x, y }
})

const strokeColor = computed(() => {
  switch (props.status) {
    case 'OUT_OF_RANGE':
      return 'var(--color-critical)'
    case 'OVERDUE':
      return 'var(--color-warning)'
    default:
      return 'var(--color-primary)'
  }
})
</script>

<template>
  <div class="sparkline-shell" aria-hidden="true">
    <svg
      class="sparkline"
      :viewBox="`0 0 ${viewBoxWidth} ${viewBoxHeight}`"
      preserveAspectRatio="xMidYMid meet"
    >
      <polyline
        v-if="points"
        class="sparkline-line"
        :points="points"
        :style="{ stroke: strokeColor }"
      />
      <circle
        v-if="lastPoint"
        class="sparkline-point"
        :cx="lastPoint.x"
        :cy="lastPoint.y"
        :r="pointRadius"
        :style="{ fill: strokeColor }"
      />
    </svg>
  </div>
</template>

<style scoped>
.sparkline-shell {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  padding: 6px 0;
}

.sparkline {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.sparkline-line {
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
