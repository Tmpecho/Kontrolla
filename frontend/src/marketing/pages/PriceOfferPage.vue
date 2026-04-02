<script setup lang="ts">
import { reactive, ref } from 'vue'

const form = reactive({
  fullName: '',
  companyName: '',
  email: '',
  phone: '',
  locationCount: '',
  needs: '',
  message: '',
})

const isSubmitted = ref(false)

function onSubmit() {
  isSubmitted.value = true
}
</script>

<template>
  <div class="price-offer-page">
    <main class="page-main">
      <section class="page-intro">
        <h1>Get a price offer</h1>
        <p class="lead">
          Share a few details about your operation and we can prepare a price offer for food
          compliance, alcohol compliance, or a combined setup.
        </p>
      </section>

      <section class="page-content">
        <div class="info-column">
          <div class="info-block">
            <h2>What we price against</h2>
            <p>
              The offer depends on how many locations you run, which compliance areas you need,
              and how much operational follow-up should be managed in the system.
            </p>
          </div>

          <div class="info-list">
            <div class="info-list-row">Single-site restaurants and bars</div>
            <div class="info-list-row">Multi-location hospitality groups</div>
            <div class="info-list-row">Food safety only, alcohol only, or both</div>
            <div class="info-list-row">Operational routines, deviations, and documentation</div>
          </div>
        </div>

        <div class="form-column">
          <div v-if="isSubmitted" class="confirmation-panel">
            <h2>Request received</h2>
            <p>
              Thanks. Your pricing request is ready for follow-up. The next step is to connect this
              form to a real contact channel or backend endpoint.
            </p>
            <RouterLink :to="{ name: 'landing' }" class="secondary-action">
              Return to homepage
            </RouterLink>
          </div>

          <form v-else class="offer-form" @submit.prevent="onSubmit">
            <div class="field-row">
              <div class="field">
                <label for="full-name">Full name</label>
                <input id="full-name" v-model="form.fullName" type="text" autocomplete="name" required />
              </div>

              <div class="field">
                <label for="company-name">Company</label>
                <input id="company-name" v-model="form.companyName" type="text" autocomplete="organization" required />
              </div>
            </div>

            <div class="field-row">
              <div class="field">
                <label for="email">Work email</label>
                <input id="email" v-model="form.email" type="email" autocomplete="email" required />
              </div>

              <div class="field">
                <label for="phone">Phone</label>
                <input id="phone" v-model="form.phone" type="tel" autocomplete="tel" required />
              </div>
            </div>

            <div class="field-row">
              <div class="field">
                <label for="location-count">Number of locations</label>
                <input id="location-count" v-model="form.locationCount" type="text" inputmode="numeric" required />
              </div>

              <div class="field">
                <label for="needs">Compliance scope</label>
                <select id="needs" v-model="form.needs" required>
                  <option disabled value="">Select scope</option>
                  <option value="food">Food compliance</option>
                  <option value="alcohol">Alcohol compliance</option>
                  <option value="both">Food and alcohol compliance</option>
                </select>
              </div>
            </div>

            <div class="field">
              <label for="message">What do you need help with?</label>
              <textarea
                id="message"
                v-model="form.message"
                rows="7"
                required
                placeholder="Describe your setup, current routines, and what you want to digitize first."
              />
            </div>

            <div class="form-actions">
              <button type="submit" class="primary-action">Send request</button>
              <p class="form-note">
                This form currently stores the request locally in the UI only.
              </p>
            </div>
          </form>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.price-offer-page {
  color: var(--color-text-primary);
}

.page-main {
  width: 100%;
}

.page-main {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.page-intro {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 40px 0 8px;
}

.page-intro h1,
.page-intro p,
.info-block h2,
.info-block p,
.confirmation-panel h2,
.confirmation-panel p,
.form-note {
  margin: 0;
}

.page-intro h1 {
  max-width: 12ch;
  font-size: clamp(2.2rem, 4vw, 4rem);
  line-height: 1;
  letter-spacing: -0.04em;
}

.lead {
  max-width: 62ch;
  font-size: 1rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.page-content {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.1fr);
  gap: 32px;
  align-items: start;
}

.info-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.offer-form,
.confirmation-panel {
  padding: 24px;
  border: 1px solid var(--color-border-muted);
  background-color: var(--color-container);
}

.info-block h2,
.confirmation-panel h2 {
  font-size: 1.25rem;
}

.info-block p,
.confirmation-panel p {
  margin-top: 10px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.info-list-row {
  padding: 16px 0;
  border-top: 1px solid var(--color-border-muted);
}

.info-list-row:first-child {
  padding-top: 0;
  border-top: none;
}

.field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.offer-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field label {
  font-size: 0.875rem;
  font-weight: 600;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 0.875rem 0.875rem;
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  background-color: var(--color-container);
  color: var(--color-text-primary);
  font: inherit;
  box-sizing: border-box;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-color: transparent;
}

.field textarea {
  min-height: 180px;
  resize: vertical;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  padding-top: 8px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.primary-action {
  background-color: var(--color-primary);
  color: var(--color-white);
}

.secondary-action {
  border-color: var(--color-border);
  background-color: var(--color-container);
  color: var(--color-text-primary);
}

.form-note {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

@media (max-width: 860px) {
  .page-content,
  .field-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-intro {
    padding-top: 20px;
  }

  .info-block,
  .offer-form,
  .confirmation-panel {
    padding: 20px;
  }
}
</style>
