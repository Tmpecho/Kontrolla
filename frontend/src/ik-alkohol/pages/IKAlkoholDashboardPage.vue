<script setup lang="ts">
  import AgeVerificationTile from '@/ik-alkohol/components/AgeVerificationTile.vue'
  import ImportantDocumentsTile from '@/ik-alkohol/components/ImportantDocumentsTile.vue'
  import ServingHoursTile from '@/ik-alkohol/components/ServingHoursTile.vue'
  import DeviationsTile from '@/shared/components/DeviationsTile.vue'
</script>

<template>
  <div class="dashboard-page">
    <section class="dashboard-section">
      <div>
        <h1>IK-alkohol Dashboard</h1>
        <p>Overview over alcohol compliance.</p>
      </div>
    </section>

    <section class="dashboard-section">
      <DeviationsTile
        add-deviation-to="/app/ik-alkohol/deviation/form"
        class="dashboard-tile"
        deviation-page-to="/app/ik-alkohol/deviation"
        service-area="IK_ALKOHOL"
      />
    </section>

    <div class="row-container">
      <section class="dashboard-section dashboard-section-documents">
        <ImportantDocumentsTile class="dashboard-tile" />
      </section>

      <div class="stacked-column">
        <section class="dashboard-section">
          <AgeVerificationTile class="dashboard-tile" />
        </section>

        <section class="dashboard-section">
          <ServingHoursTile
            :dashboard-to="{ name: 'ik-alkohol-dashboard' }"
            :edit-to="{ name: 'ik-alkohol-serving-hours-edit' }"
            class="dashboard-tile"
          />
        </section>
      </div>
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
  box-shadow: var(--shadow-elevated);

}

.row-container {
  display: flex;
  flex-direction: row;
  gap: 24px;
}

.dashboard-section-documents {
  flex: 1.15;
}

.stacked-column {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 24px;
}

@media (max-width: 960px) {
  .row-container {
    flex-direction: column;
  }

  .stacked-column {
    gap: 24px;
  }
}

</style>
