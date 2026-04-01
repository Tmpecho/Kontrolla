<script setup lang="ts">
import BaseButton from '@/shared/components/BaseButton.vue'
</script>

<template>
  <div class="upload-page">
    <header class="page-header">
      <h1>Upload new document</h1>
      <p class="page-subtitle">
        This placeholder page will become the upload and metadata flow for important alcohol
        documents.
      </p>
    </header>

    <section class="placeholder-panel">
      <h2>Upload flow coming next</h2>
      <p>
        The next step will support file upload, linked staff or entity records, issue dates,
        renewal dates, and validation status.
      </p>

      <RouterLink :to="{ name: 'ik-alkohol-documents' }" class="back-link">
        <BaseButton class="back-button" type="button">Back to important documents</BaseButton>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.upload-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header h1,
.page-subtitle,
.placeholder-panel h2,
.placeholder-panel p {
  margin: 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  max-width: 64ch;
}

.placeholder-panel {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}

.back-link {
  align-self: flex-start;
  margin-top: 8px;
  text-decoration: none;
}

.back-button {
  width: auto;
}

.back-button :deep(.button) {
  width: auto;
  justify-content: center;
}
</style>
