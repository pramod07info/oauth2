import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "e2e",
  fullyParallel: false,
  retries: process.env["CI"] ? 1 : 0,
  reporter: "list",
  use: {
    baseURL: "http://localhost:4200",
    trace: "retain-on-failure",
    ...devices["Desktop Chrome"],
    channel: "chromium",
  },
  webServer: {
    command: "npm start",
    url: "http://localhost:4200",
    reuseExistingServer: !process.env["CI"],
    timeout: 120000,
  },
});
