import { test, expect } from '@playwright/test'

test('shows the landing page and opens the login page', async ({ page }) => {
  await page.goto('/')

  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Food and alcohol compliance, kept in one place.',
    }),
  ).toBeVisible()

  await page
    .getByRole('navigation', { name: 'Marketing navigation' })
    .getByRole('link', { name: 'Sign in' })
    .click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Kontrolla' })).toBeVisible()
  await expect(page.getByLabel('Email or Username')).toBeVisible()
  await expect(page.getByLabel('Password')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible()
})
