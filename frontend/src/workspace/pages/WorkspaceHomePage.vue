<script setup lang="ts">
</script>

<template>
  <div class="workspace-page">
    <section class="workspace-section">
      <header class="workspace-header">
        <h1>Workspace</h1>
        <p>The starting point for daily routines, follow-up, documents, and reporting.</p>
      </header>

      <p class="workspace-introduction">
        This workspace will become the operational home for internal control across food safety and
        alcohol compliance.
      </p>
    </section>

    <section class="workspace-section">
      <div class="service-grid">
        <RouterLink :to="{ name: 'ik-mat-dashboard' }" class="service-tile">
          <h2>IK-mat</h2>
          <p>Checklists, food safety routines, temperatures, and kitchen follow-up.</p>
        </RouterLink>

        <RouterLink :to="{ name: 'ik-alkohol-dashboard' }" class="service-tile">
          <h2>IK-alkohol</h2>
          <p>Alcohol compliance routines, training follow-up, and service controls.</p>
        </RouterLink>
      </div>
    </section>

    <section class="workspace-section">
      <h2>Daily work</h2>
      <p>Routine execution, checklists, temperature logs, and shift-level tasks will live here.</p>
    </section>

    <section class="workspace-section">
      <h2>Follow-up</h2>
      <p>Deviation handling, incident review, and manager follow-up will be added here.</p>
    </section>

    <section class="workspace-section">
      <h2>Documentation and reports</h2>
      <p>Policies, certifications, exports, and inspection-ready reporting will be available here.</p>
    </section>
  </div>
</template>

<style scoped>
.workspace-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.workspace-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workspace-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workspace-header h1,
.workspace-header p,
.workspace-section h2,
.workspace-section p {
  margin: 0;
}

.workspace-introduction {
  max-width: 70ch;
}

.service-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.service-tile {
  display: flex;
  min-height: 120px;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  text-decoration: none;
}

.service-tile:hover {
  border-color: var(--color-primary);
}

.service-tile h2,
.service-tile p {
  margin: 0;
}

.service-tile h2 {
  color: var(--color-primary);
  font-size: 1.5rem;
}
</style>
