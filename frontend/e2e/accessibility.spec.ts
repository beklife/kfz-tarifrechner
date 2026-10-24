import AxeBuilder from '@axe-core/playwright';
import { Page, expect, test } from '@playwright/test';
import { RechnerPage } from './rechner.page';

/**
 * Automatische Prüfung mit axe-core gegen WCAG 2.1 A und AA.
 * Wir prüfen jeden wichtigen Zustand: leer, mit Fehlern, mit Ergebnis, Fehlermeldung, Über-Seite.
 * Ziel: null Verstöße. (axe findet etwa 30–50 % der Probleme. Den Rest prüft man von Hand,
 * z. B. mit Tastatur und Screenreader.)
 */
async function expectNoAxeViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  // Bei Fehlern sieht man im Testbericht direkt Regel, Beschreibung und betroffene Elemente.
  const summary = results.violations.map((violation) => ({
    rule: violation.id,
    description: violation.help,
    elements: violation.nodes.map((node) => node.target.join(' ')),
  }));
  expect(summary).toEqual([]);
}

test.describe('Barrierefreiheit (axe)', () => {
  test('Rechner: leeres Formular', async ({ page }) => {
    await new RechnerPage(page).open();
    await expectNoAxeViolations(page);
  });

  test('Rechner: Formular mit Fehlermeldungen', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.submitButton.click();
    await expect(page.locator('.error-message')).toHaveCount(4);

    await expectNoAxeViolations(page);
  });

  test('Rechner: mit Ergebnis und Tabelle', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.fill('Rheinmobil Touring 500 (Risikoklasse 3)', 40, 5, 'Teilkasko');
    await rechner.submitButton.click();
    await expect(rechner.resultHeading).toBeVisible();

    await expectNoAxeViolations(page);
  });

  test('Rechner: Fahrzeugliste nicht ladbar', async ({ page }) => {
    await page.route('**/api/vehicles', (route) => route.fulfill({ status: 503, json: { status: 503 } }));
    await page.goto('/rechner');
    await expect(page.getByRole('button', { name: 'Erneut versuchen' })).toBeVisible();

    await expectNoAxeViolations(page);
  });

  test('Über das Projekt', async ({ page }) => {
    await page.goto('/ueber');
    await expect(page.getByRole('heading', { level: 1, name: 'Über das Projekt' })).toBeVisible();

    await expectNoAxeViolations(page);
  });

  test('Rechner: mobile Breite (375 px)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 800 });
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.fill('Rheinmobil Touring 500 (Risikoklasse 3)', 40, 5, 'Teilkasko');
    await rechner.submitButton.click();
    await expect(rechner.resultHeading).toBeVisible();

    await expectNoAxeViolations(page);
  });
});
