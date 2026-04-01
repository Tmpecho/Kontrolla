<script lang="ts" setup>
import complianceIllustrationUrl from '@/assets/illustrations/compliance-operations.svg'
</script>

<template>
  <div class="landing-page">
    <header id="top" class="topbar">
      <RouterLink :to="{ name: 'landing' }" class="brand">Kontrolla</RouterLink>
      <RouterLink :to="{ name: 'login' }" class="login-link">Sign in</RouterLink>
    </header>

    <main class="landing-main">
      <section class="hero">
        <div class="hero-copy">
          <h1>Food and alcohol compliance, kept in one place.</h1>
          <p class="lead">
            Kontrolla helps restaurants and bars manage internal control,
            routines, deviations, and documentation without paper trails or scattered spreadsheets.
          </p>
          <p class="supporting-copy">
            The product is built for teams that need clear ownership, inspection-ready records, and
            a practical workflow for both food safety and alcohol compliance.
          </p>

          <div class="hero-actions">
            <a class="primary-action" href="#price-offer">
              Get a price offer
            </a>
            <RouterLink :to="{ name: 'login' }" class="secondary-action">
              Sign in
            </RouterLink>
          </div>
        </div>

        <figure class="hero-visual">
          <img
            :src="complianceIllustrationUrl"
            alt="Illustration of compliance routines, documentation, and management overview"
            class="hero-image"
          />
          <figcaption class="hero-caption">
            Daily routines, follow-up, and documentation aligned in one operating view.
          </figcaption>
        </figure>
      </section>

      <section class="content-section accent-section">
        <div class="section-heading">
          <h2>Built for operational control</h2>
          <p>
            A straightforward system for the compliance work that needs to happen every day, not
            just when an inspection is near.
          </p>
        </div>

        <div class="feature-grid">
          <div class="feature-column">
            <h3>Food compliance</h3>
            <p>
              Keep cleaning routines, temperature checks, and corrective actions documented in a
              format the whole team can follow.
            </p>
          </div>

          <div class="feature-column">
            <h3>Alcohol compliance</h3>
            <p>
              Track serving rules, training, and follow-up so responsible alcohol management stays
              visible across locations and shifts.
            </p>
          </div>

          <div class="feature-column">
            <h3>Management visibility</h3>
            <p>
              See what has been completed, what still needs attention, and where the next action
              belongs.
            </p>
          </div>
        </div>
      </section>

      <section id="price-offer" class="cta-section cta-accent-section">
        <div class="cta-copy">
          <h2>Need a price offer?</h2>
          <p>
            We can walk through your setup and give you a price offer based on the number of
            locations, workflows, and compliance needs.
          </p>
          <p class="cta-detail">
            Prepare your number of locations, whether you need food compliance, alcohol
            compliance, or both, and which routines you want to digitize first.
          </p>
        </div>

        <div class="cta-actions">
          <a class="primary-action" href="#top">Review the offer basis</a>
          <RouterLink :to="{ name: 'login' }" class="secondary-action">
            Sign in
          </RouterLink>
        </div>
      </section>
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
        <a class="footer-link" href="#price-offer">Get a price offer</a>
        <RouterLink :to="{ name: 'login' }" class="footer-link">Sign in</RouterLink>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.landing-page {
  min-height: 100vh;
  padding: 20px;
  color: var(--color-text-primary);
}

.topbar,
.landing-main {
  width: min(1120px, 100%);
  margin: 0 auto;
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

.login-link {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  text-decoration: none;
}

.landing-main {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 32px;
  align-items: center;
  padding: 56px 0 24px;
}

.hero-copy h1,
.section-heading h2,
.cta-copy h2,
.feature-column h3,
.feature-column p,
.hero-copy p,
.cta-copy p,
.hero-caption {
  margin: 0;
}

.hero-copy h1 {
  max-width: 12ch;
  font-size: clamp(2.4rem, 4vw, 4.4rem);
  line-height: 0.98;
  letter-spacing: -0.04em;
}

.lead {
  max-width: 60ch;
  margin-top: 18px;
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--color-text-primary);
}

.supporting-copy {
  max-width: 58ch;
  margin-top: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.hero-actions,
.cta-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
}

.primary-action {
  background: var(--color-primary);
  color: var(--color-white);
}

.secondary-action {
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  background: var(--color-container);
}

.hero-visual {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.hero-image {
  width: min(100%, 440px);
  height: auto;
  display: block;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background-color: var(--color-container);
}

.hero-caption {
  width: min(100%, 440px);
  font-size: 0.82rem;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.content-section,
.cta-section {
  padding: 32px;
  border-top: 1px solid var(--color-border-muted);
  border-bottom: 1px solid var(--color-border-muted);
}

.accent-section {
  background-color: color-mix(in srgb, var(--color-primary) 5%, var(--color-container));
}

.cta-accent-section {
  background-color: color-mix(in srgb, var(--color-primary) 88%, #0f172a);
  border-color: color-mix(in srgb, var(--color-primary) 55%, #0f172a);
}

.section-heading {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 70ch;
}

.section-heading h2,
.cta-copy h2 {
  font-size: clamp(1.7rem, 2vw, 2.4rem);
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.section-heading p,
.cta-copy p,
.feature-column p {
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.cta-detail {
  margin-top: 12px;
}

.cta-accent-section .cta-copy h2,
.cta-accent-section .cta-copy p {
  color: var(--color-white);
}

.cta-accent-section .primary-action {
  background: var(--color-white);
  color: var(--color-primary);
}

.cta-accent-section .secondary-action {
  border-color: color-mix(in srgb, var(--color-white) 26%, transparent);
  color: var(--color-white);
  background: transparent;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.feature-column {
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.feature-column h3 {
  font-size: 1rem;
  margin-bottom: 10px;
}

.cta-section {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.cta-copy {
  max-width: 64ch;
}

.site-footer {
  width: min(1120px, 100%);
  margin: 0 auto;
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
  .hero,
  .cta-section {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero {
    padding-top: 28px;
  }

  .hero-visual {
    align-items: flex-start;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .site-footer {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .landing-page {
    padding: 16px;
  }

  .topbar {
    padding-bottom: 20px;
  }

  .hero-copy h1 {
    max-width: 100%;
  }

  .content-section,
  .cta-section {
    padding: 24px;
  }

  .hero-image,
  .hero-caption {
    width: 100%;
  }

  .primary-action,
  .secondary-action {
    width: 100%;
  }
}
</style>
