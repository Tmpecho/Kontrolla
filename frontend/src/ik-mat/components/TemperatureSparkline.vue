<script setup lang="ts">
import { computed } from 'vue'

import type {
  TemperatureAlertState,
  TemperatureLogEntry,
} from '@/ik-mat/model/temperature.types'

const props = defineProps<{
  logs: TemperatureLogEntry[]
  alertState: TemperatureAlertState
  minimumTemperature: number
  maximumTemperature: number
}>()

const viewBoxWidth = 100
const viewBoxHeight = 32
const pointRadius = 2.5
const horizontalPadding = pointRadius + 1

const orderedLogs = computed(() => {
  return [...props.logs]
    .sort(
      (left, right) => new Date(left.measuredAt).getTime() - new Date(right.measuredAt).getTime(),
    )
    .slice(-7)
})

const chartDomain = computed(() => {
  const loggedTemperatures = orderedLogs.value.map((logEntry) => logEntry.temperatureCelsius)
  const absoluteMinimum = Math.min(
    props.minimumTemperature,
    props.maximumTemperature,
    ...loggedTemperatures,
  )
  const absoluteMaximum = Math.max(
    props.minimumTemperature,
    props.maximumTemperature,
    ...loggedTemperatures,
  )
  const padding = Math.max((absoluteMaximum - absoluteMinimum) * 0.12, 1)

  return {
    minimum: absoluteMinimum - padding,
    maximum: absoluteMaximum + padding,
  }
})

function getYCoordinate(temperatureCelsius: number): number {
  const temperatureSpan = chartDomain.value.maximum - chartDomain.value.minimum || 1
  const normalizedY = (temperatureCelsius - chartDomain.value.minimum) / temperatureSpan

  return viewBoxHeight - normalizedY * (viewBoxHeight - 6) - 3
}

const points = computed(() => {
  if (orderedLogs.value.length === 0) {
    return ''
  }

  return orderedLogs.value
    .map((logEntry, index) => {
      const x =
        orderedLogs.value.length === 1
          ? viewBoxWidth / 2
          : horizontalPadding +
            (index / (orderedLogs.value.length - 1)) * (viewBoxWidth - horizontalPadding * 2)
      const y = getYCoordinate(logEntry.temperatureCelsius)

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

const minimumThresholdY = computed(() => getYCoordinate(props.minimumTemperature))
const maximumThresholdY = computed(() => getYCoordinate(props.maximumTemperature))

const strokeColor = computed(() => {
  switch (props.alertState) {
    case 'OUT_OF_RANGE':
      return 'var(--color-critical)'
    case 'OVERDUE':
    case 'DUE_SOON':
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
      <line
        class="sparkline-threshold"
        :x1="horizontalPadding"
        :x2="viewBoxWidth - horizontalPadding"
        :y1="minimumThresholdY"
        :y2="minimumThresholdY"
      />
      <line
        class="sparkline-threshold"
        :x1="horizontalPadding"
        :x2="viewBoxWidth - horizontalPadding"
        :y1="maximumThresholdY"
        :y2="maximumThresholdY"
      />
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

.sparkline-threshold {
  stroke: color-mix(in srgb, var(--color-border-muted) 72%, transparent);
  stroke-width: 1;
  stroke-dasharray: 2 2;
}
</style>
