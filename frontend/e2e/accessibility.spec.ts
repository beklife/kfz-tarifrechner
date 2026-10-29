import { expect, test } from '@playwright/test';
import { expectNoAxeViolations } from './a11y-report';
import { RechnerPage } from './rechner.page';

/**
 * Automatische Prüfung mit axe-core gegen WCAG 2.1 A und AA.
 * Wir prüfen jeden wichtigen Zustand: leer, mit Fehlern, mit Ergebnis, Fehlermeldung, Über-Seite, Bericht-Seite.
 * Ziel: null Verstöße. (axe findet etwa 30–50 % der Probleme. Den Rest prüft man von Hand,
 * z. B. mit Tastatur und Screenreader.)
 * Die Ergebnisse landen in public/a11y-report.json und werden auf /barrierefreiheit angezeigt.
 */
test.describe('Barrierefreiheit (axe)', () => {
  test('Rechner: leeres Formular', async ({ page }) => {
    await new RechnerPage(page).open();
    await expectNoAxeViolations(page, 'rechner-leer');
  });

  test('Rechner: Formular mit Fehlermeldungen', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.submitButton.click();
    await expect(page.locator('.error-message')).toHaveCount(4);

    await expectNoAxeViolations(page, 'rechner-fehler');
  });

  test('Rechner: mit Ergebnis und Tabelle', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.fill('Rheinmobil Touring 500 (Risikoklasse 3)', 40, 5, 'Teilkasko');
    await rechner.submitButton.click();
    await expect(rechner.resultHeading).toBeVisible();

    await expectNoAxeViolations(page, 'rechner-ergebnis');
  });

  test('Rechner: Fahrzeugliste nicht ladbar', async ({ page }) => {
    await page.route('**/api/vehicles', (route) => route.fulfill({ status: 503, json: { status: 503 } }));
    await page.goto('/rechner');
    await expect(page.getByRole('button', { name: 'Erneut versuchen' })).toBeVisible();

    await expectNoAxeViolations(page, 'rechner-fehler-laden');
  });

  test('Über das Projekt', async ({ page }) => {
    await page.goto('/ueber');
    await expect(page.getByRole('heading', { level: 1, name: 'Über das Projekt' })).toBeVisible();

    await expectNoAxeViolations(page, 'ueber');
  });

  test('Rechner: mobile Breite (375 px)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 800 });
    const rechner = new RechnerPage(page);
    await rechner.open();
    await rechner.fill('Rheinmobil Touring 500 (Risikoklasse 3)', 40, 5, 'Teilkasko');
    await rechner.submitButton.click();
    await expect(rechner.resultHeading).toBeVisible();

    await expectNoAxeViolations(page, 'rechner-mobil');
  });

  test('Barrierefreiheit: Bericht-Seite', async ({ page }) => {
    await page.goto('/barrierefreiheit');
    await expect(page.getByRole('table', { name: 'Ergebnis je Seiten-Zustand' })).toBeVisible();

    await expectNoAxeViolations(page, 'barrierefreiheit');
  });
});
