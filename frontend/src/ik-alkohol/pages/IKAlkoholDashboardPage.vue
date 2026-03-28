<script setup lang="ts">
  import ServingHoursTile from '@/ik-alkohol/components/ServingHoursTile.vue'
</script>

<template>
  <div class="dashboard-page">
    <section class="dashboard-section">
      <div>
        <h1>IK-alkohol Dashboard</h1>
        <p>Overview over alchohol compliance.</p>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="dashboard-tile">
        <h2>Upcoming Tasks</h2>
        <p>Upcoming tasks ...</p>
      </div>
    </section>

      <section class="dashboard-section">
      

      <div class="dashboard-tile">
        <h2>Diviations</h2>
        <p>Diviations ...</p>
      </div>
    </section>

    <div class="row-container">
      <section class="dashboard-section">
        <RouterLink :to="{ name: 'ik-alkohol-documents' }" class="tile-link">
          <div class="dashboard-tile dashboard-tile-link">
            <h2>Documents</h2>
            <p>Overview over alchohol compliance.</p>
          </div>
        </RouterLink>
      </section>

      <section class="dashboard-section">
        <ServingHoursTile class="dashboard-tile"/>
      </section>
    </div>

  </div>
</template>

<style scoped>

.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.dashboard-tile {
  display: flex;
  min-height: 120px;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  text-decoration: none;
}

.tile-link {
  text-decoration: none;
}

.dashboard-tile-link:hover {
  border-color: var(--color-primary);
}

.row-container {
  display: flex;
  flex-direction: row;
  gap: 24px
}

</style>