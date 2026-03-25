import { test, expect } from '@playwright/test'

test('shows the landing page and opens the login page', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { level: 1, name: 'Kontrolla' })).toBeVisible()

  await page.getByRole('link', { name: 'Go to login' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible()
})
