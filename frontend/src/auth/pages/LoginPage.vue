<script setup lang="ts">
import { ref } from 'vue'
import Input from '@/shared/components/Input.vue'
import Button from '@/shared/components/Button.vue'

const email = ref('')
const password = ref('')

const handleLogin = () => {
  console.log('Authenticating:', email.value)
}
</script>

<template>
  <div class="page-layout">
    <div class="login-container">
      <form @submit.prevent="handleLogin" class="form-box">
        <div class="form-content">
          <h1 class="title">Log in</h1>
          <p class="subtitle">
            Don't have an account? <a href="#" class="text-link">Create an account</a>
          </p>

          <div class="input-wrapper">
            <Input id="email" label="Email address" type="email" v-model="email" />
            <a href="#" class="text-link forgot-link">Forgot ID?</a>
          </div>

          <div class="input-wrapper">
            <Input id="password" label="Password" type="password" v-model="password" />
          </div>

          <div class="checkbox-wrapper">
            <input type="checkbox" id="remember" class="custom-checkbox" />
            <label for="remember" class="checkbox-label">Remember ID</label>
          </div>
        </div>

        <div class="form-actions">
          <div class="primary-action">
            <Button type="submit" class="continue-btn">
              Continue <span class="arrow">&rarr;</span>
            </Button>
          </div>
        </div>
      </form>

      <div class="help-footer">
        Need help? <a href="#" class="text-link">Contact the help desk</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f4f4f4;
  padding: 2rem;
  font-family:
    'IBM Plex Sans',
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}

.login-container {
  width: 100%;
  max-width: 480px;
}

.form-box {
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.form-content {
  padding: 3rem 3rem 1.5rem 3rem;
}

.title {
  font-size: 2rem;
  font-weight: 300;
  color: #161616;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  font-size: 0.875rem;
  color: #525252;
  margin: 0 0 2rem 0;
}

.text-link {
  color: #0f62fe;
  text-decoration: none;
  font-size: 0.875rem;
}

.text-link:hover {
  text-decoration: underline;
}

.input-wrapper {
  position: relative;
  margin-bottom: 1.5rem;
}

.forgot-link {
  position: absolute;
  top: 0;
  right: 0;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  margin-top: 1rem;
  margin-bottom: 1.5rem;
}

.custom-checkbox {
  margin: 0;
}

.checkbox-label {
  margin-left: 0.5rem;
  font-size: 0.875rem;
  color: #161616;
  cursor: pointer;
}

.form-actions {
  display: flex;
  width: 100%;
  height: 4rem;
}

.primary-action {
  flex: 1;
}

.primary-action :deep(button),
.continue-btn {
  width: 100%;
  height: 100%;
  border-radius: 0;
  background-color: #0f62fe;
  color: white;
  border: none;
  font-size: 0.875rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-action :deep(button):hover,
.continue-btn:hover {
  background-color: #0353e9;
}

.help-footer {
  margin-top: 1rem;
  font-size: 0.875rem;
  color: #525252;
}
</style>
