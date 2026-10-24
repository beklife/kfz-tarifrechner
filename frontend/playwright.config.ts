import { defineConfig, devices } from '@playwright/test';

// Ohne E2E_BASE_URL startet Playwright alles selbst: beide Java-Services (fertige JARs) und den Angular-Dev-Server.
// Mit E2E_BASE_URL (z. B. http://localhost:8080 bei docker compose) testet es eine schon laufende Umgebung.
const externalBaseUrl = process.env['E2E_BASE_URL'];
const baseURL = externalBaseUrl ?? 'http://localhost:4300';
const isCi = !!process.env['CI'];

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  reporter: isCi ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    locale: 'de-DE',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: externalBaseUrl
    ? undefined
    : [
        {
          command: 'java -jar ../vehicle-service/target/vehicle-service-0.0.1-SNAPSHOT.jar',
          url: 'http://localhost:8081/actuator/health/readiness',
          reuseExistingServer: !isCi,
          timeout: 120_000,
        },
        {
          command: 'java -jar ../quote-service/target/quote-service-0.0.1-SNAPSHOT.jar',
          url: 'http://localhost:8082/actuator/health/readiness',
          reuseExistingServer: !isCi,
          timeout: 120_000,
        },
        {
          // Port 4300 statt 4200, damit es nicht mit einem laufenden "npm start" kollidiert.
          command: 'npx ng serve --port 4300',
          url: 'http://localhost:4300',
          reuseExistingServer: !isCi,
          timeout: 120_000,
        },
      ],
});
