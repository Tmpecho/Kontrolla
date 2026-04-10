<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import type { WorkspaceStartupStatus } from '@/auth/model/auth.types'

const props = defineProps<{
  status: WorkspaceStartupStatus
  errorMessage: string | null
  startedAt: number | null
}>()

const emit = defineEmits<{
  retry: []
}>()

const now = ref(Date.now())
let nowIntervalId: number | null = null

const elapsedMs = computed(() => {
  if (props.startedAt === null) {
    return 0
  }

  return Math.max(0, now.value - props.startedAt)
})

const showLongWaitMessage = computed(() => elapsedMs.value >= 30_000)
const showRetryAction = computed(() => {
  return props.status === 'error' || elapsedMs.value >= 90_000
})

const title = computed(() => {
  if (props.status === 'error') {
    return 'Unable to start workspace'
  }

  if (props.status === 'bootstrapping-workspace') {
    return 'Loading workspace...'
  }

  return 'Starting workspace...'
})

const description = computed(() => {
  if (props.status === 'error') {
    return props.errorMessage ?? 'Kontrolla could not finish starting the workspace.'
  }

  if (props.status === 'bootstrapping-workspace') {
    return 'Loading your organization and establishment context.'
  }

  if (showLongWaitMessage.value) {
    return 'Kontrolla is still starting. This can take a little while.'
  }

  return 'Preparing your workspace and checking that the backend is ready.'
})

function startClock() {
  if (nowIntervalId !== null || typeof window === 'undefined') {
    return
  }

  nowIntervalId = window.setInterval(() => {
    now.value = Date.now()
  }, 1_000)
}

function stopClock() {
  if (nowIntervalId === null || typeof window === 'undefined') {
    return
  }

  window.clearInterval(nowIntervalId)
  nowIntervalId = null
}

onMounted(() => {
  startClock()
})

onBeforeUnmount(() => {
  stopClock()
})
</script>

<template>
  <section class="startup-state" :data-status="status" aria-live="polite">
    <div class="startup-state__card">
      <div v-if="status !== 'error'" class="startup-state__spinner" aria-hidden="true"></div>
      <h1 class="startup-state__title">{{ title }}</h1>
      <p class="startup-state__description">{{ description }}</p>
      <button
        v-if="showRetryAction"
        type="button"
        class="startup-state__retry"
        @click="emit('retry')"
      >
        Retry now
      </button>
    </div>
  </section>
</template>

<style scoped>
.startup-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
}

.startup-state__card {
  width: min(420px, 100%);
  padding: 32px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-container);
  box-shadow: var(--shadow-elevated);
  text-align: center;
}

.startup-state__spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 16px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: startup-spin 0.9s linear infinite;
}

.startup-state__title {
  margin: 0;
  font-size: 1.25rem;
  color: var(--color-text-primary);
}

.startup-state__description {
  margin: 12px 0 0;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.startup-state__retry {
  margin-top: 20px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-surface);
  color: var(--color-text-primary);
  padding: 10px 14px;
  font: inherit;
  cursor: pointer;
}

.startup-state__retry:hover {
  background-color: color-mix(in srgb, var(--color-surface) 90%, black);
}

.startup-state__retry:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

@keyframes startup-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
