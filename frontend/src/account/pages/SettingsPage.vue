<template>
  <div class="settings-page">
    <header class="page-header">
      <h1>Settings</h1>
      <p>Application and account settings will be added here.</p>
    </header>

    <section class="placeholder-panel">
      <p>This page is a placeholder for future user and workspace settings.</p>
    </section>
  </div>
</template>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header,
.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-header h1,
.page-header p,
.placeholder-panel p {
  margin: 0;
}

.placeholder-panel {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
}
</style>
