<script setup lang="ts">
function getCutoffDate(years: number) {
  const now = new Date()

  return new Date(now.getFullYear() - years, now.getMonth(), now.getDate())
}

function formatDate(value: Date) {
  return new Intl.DateTimeFormat('nb-NO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(value)
}

const cutoffEighteen = getCutoffDate(18)
const cutoffTwenty = getCutoffDate(20)
</script>

<template>
  <div class="age-verification-tile">
    <div>
      <h2>Age verification</h2>
      <p class="tile-subtitle">Birth date cutoffs to use when checking age today.</p>
    </div>

    <div class="cutoff-grid">
      <div class="cutoff-card">
        <p class="cutoff-label">18 years</p>
        <p class="cutoff-date">{{ formatDate(cutoffEighteen) }}</p>
        <p class="cutoff-hint">Beer and wine.</p>
      </div>

      <div class="cutoff-card">
        <p class="cutoff-label">20 years</p>
        <p class="cutoff-date">{{ formatDate(cutoffTwenty) }}</p>
        <p class="cutoff-hint">Spirits.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.age-verification-tile {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
}

.age-verification-tile h2,
.tile-subtitle,
.cutoff-label,
.cutoff-date,
.cutoff-hint {
  margin: 0;
}

.tile-subtitle,
.cutoff-hint {
  color: var(--color-text-secondary);
}

.tile-subtitle {
  margin-top: 4px;
}

.cutoff-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.cutoff-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-surface);
}

.cutoff-label {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.cutoff-date {
  color: var(--color-text-primary);
  font-size: 1rem;
  font-weight: 600;
}

@media (max-width: 720px) {
  .cutoff-grid {
    grid-template-columns: 1fr;
  }
}
</style>
