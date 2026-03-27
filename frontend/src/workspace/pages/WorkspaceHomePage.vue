<script setup lang="ts">
</script>

<template>
  <section>
    <header>
      <h1>Workspace</h1>
      <p>The starting point for daily routines, follow-up, documents, and reporting.</p>
    </header>

    <p>
      This workspace will become the operational home for internal control across food safety and
      alcohol compliance.
    </p>
  </section>

  <p>
    <RouterLink :to="{ name: 'ik-mat-dashboard'}">IK-mat</RouterLink>
    <RouterLink :to="{ name: 'ik-alkohol-dashboard'}">IK-alkohol</RouterLink>
  </p>

  <section>
    <h2>Daily work</h2>
    <p>Routine execution, checklists, temperature logs, and shift-level tasks will live here.</p>
  </section>

  <section>
    <h2>Follow-up</h2>
    <p>Deviation handling, incident review, and manager follow-up will be added here.</p>
  </section>

  <section>
    <h2>Documentation and reports</h2>
    <p>Policies, certifications, exports, and inspection-ready reporting will be available here.</p>
  </section>
</template>