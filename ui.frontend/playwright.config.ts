import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testMatch: /.*\.e2e\.spec\.ts/,
  timeout: 30000,
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry'
  },
  projects: [
    {
      name: 'msedge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' }
    }
  ]
});
