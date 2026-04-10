import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      coverage: {
        provider: 'v8',
        reportsDirectory: './coverage',
        reporter: ['text', 'html', 'json-summary'],
        include: ['src/**/*.{ts,vue}'],
        exclude: [
          'src/**/*.d.ts',
          'src/**/__tests__/**',
          'src/**/*.spec.ts',
          'src/main.ts',
        ],
      },
    },
  }),
)
