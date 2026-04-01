<template>
  <div class="public-layout">
    <header class="topbar">
      <RouterLink :to="{ name: 'landing' }" class="brand">Kontrolla</RouterLink>

      <nav class="topbar-actions" aria-label="Marketing navigation">
        <RouterLink :to="{ name: 'price-offer' }" class="topbar-link">Get a price offer</RouterLink>
        <RouterLink :to="{ name: 'login' }" class="topbar-link">Sign in</RouterLink>
      </nav>
    </header>

    <main class="public-content">
      <RouterView />
    </main>

    <footer class="site-footer">
      <div class="footer-branding">
        <p class="footer-name">Kontrolla</p>
        <p class="footer-description">
          Compliance management for food safety and alcohol control across hospitality operations.
        </p>
        <p class="footer-copyright">© 2026 Kontrolla. All rights reserved.</p>
      </div>

      <div class="footer-links">
        <RouterLink :to="{ name: 'price-offer' }" class="footer-link">Get a price offer</RouterLink>
        <RouterLink :to="{ name: 'login' }" class="footer-link">Sign in</RouterLink>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  padding: 20px;
  color: var(--color-text-primary);
}

.topbar,
.public-content,
.site-footer {
  width: min(1120px, 100%);
  margin: 20px auto;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 28px;
}

.brand {
  color: var(--color-text-primary);
  font-size: 1rem;
  font-weight: 700;
  text-decoration: none;
}

.topbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.topbar-link {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  text-decoration: none;
}

.public-content {
  flex: 1;
}

.site-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 0 12px;
  border-top: 1px solid var(--color-border-muted);
}

.footer-branding,
.footer-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footer-name,
.footer-description,
.footer-copyright {
  margin: 0;
}

.footer-name {
  font-size: 0.95rem;
  font-weight: 700;
}

.footer-description {
  max-width: 48ch;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.footer-copyright {
  padding-top: 8px;
  border-top: 1px solid var(--color-border-muted);
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.footer-link {
  color: var(--color-text-primary);
  text-decoration: none;
  font-size: 0.875rem;
}

@media (max-width: 920px) {
  .site-footer {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .public-layout {
    padding: 16px;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
    padding-bottom: 20px;
  }
}
</style>
