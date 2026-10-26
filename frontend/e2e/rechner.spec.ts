import { expect, test } from '@playwright/test';
import { RechnerPage } from './rechner.page';

const BERGSPORT_GT = 'Alpenwagen Bergsport GT (Risikoklasse 5)';

test.describe('Tarifrechner', () => {
  test('berechnet einen Preis mit den echten Backends', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();

    // 600 € × 1,60 (22 Jahre) × 1,70 (Vollkasko) × 0,94 (2 Jahre = 6 %) = 1.534,08 €
    await rechner.fill(BERGSPORT_GT, 22, 2, 'Vollkasko');
    await rechner.submitButton.click();

    await expect(rechner.resultHeading).toBeFocused();
    await expect(rechner.price('Jahrespreis')).toHaveText('1.534,08 €');
    await expect(rechner.price('Monatspreis')).toHaveText('127,84 €');
    await expect(rechner.liveRegion).toContainText('Ergebnis: 1.534,08 € pro Jahr, 127,84 € pro Monat.');

    const table = page.getByRole('table', { name: 'So setzt sich der Jahrespreis zusammen' });
    await expect(table.locator('tbody tr')).toHaveCount(4);
    await expect(table).toContainText('Altersfaktor (18–24 Jahre)');
    await expect(table).toContainText('Schadenfreiheitsrabatt: 2 Jahre × 3 % = 6 %');
  });

  test('zeigt alle Fehler nach dem Absenden und fokussiert das erste Feld', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();

    await rechner.submitButton.click();

    await expect(rechner.vehicle).toBeFocused();
    await expect(rechner.vehicle).toHaveAttribute('aria-invalid', 'true');
    // Pflichtfelder: sichtbarer Stern + "erforderlich" für Screenreader
    await expect(rechner.vehicle).toHaveAttribute('aria-required', 'true');
    await expect(rechner.coverage('Haftpflicht')).toHaveAttribute('required', '');
    await expect(page.getByText('Felder mit * sind Pflichtfelder.')).toBeVisible();
    // Die Fehlermeldung ist über aria-describedby mit dem Feld verbunden.
    await expect(rechner.driverAge).toHaveAccessibleDescription(/Bitte das Alter angeben\./);
    await expect(page.getByText('Bitte eine Versicherungsart auswählen.')).toBeVisible();
    await expect(rechner.resultHeading).toHaveCount(0);
  });

  test('zeigt Fehler erst nach dem Verlassen des Feldes', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();

    await rechner.driverAge.fill('17');
    await expect(page.getByText('Das Alter muss mindestens 18 sein.')).toHaveCount(0);

    await rechner.driverAge.blur();
    await expect(page.getByText('Das Alter muss mindestens 18 sein.')).toBeVisible();
  });

  test('prüft unfallfreie Jahre gegen das Alter (Cross-Field)', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();

    await rechner.fill(BERGSPORT_GT, 30, 13, 'Haftpflicht');
    await rechner.submitButton.click();

    await expect(rechner.accidentFreeYears).toBeFocused();
    await expect(rechner.accidentFreeYears).toHaveAccessibleDescription(
      /höchstens 12 unfallfreie Jahre möglich/,
    );
  });

  test('ist komplett per Tastatur bedienbar', async ({ page }) => {
    const rechner = new RechnerPage(page);
    await rechner.open();

    // Erster Tab-Stopp: der Skip-Link
    await page.keyboard.press('Tab');
    const skipLink = page.getByRole('link', { name: 'Zum Inhalt springen' });
    await expect(skipLink).toBeFocused();
    await expect(skipLink).toBeInViewport();

    await rechner.vehicle.focus();
    await page.keyboard.press('ArrowDown'); // erstes Fahrzeug
    await page.keyboard.press('Tab');
    await expect(rechner.driverAge).toBeFocused();
    await page.keyboard.type('40');
    await page.keyboard.press('Tab');
    await expect(rechner.accidentFreeYears).toBeFocused();
    await page.keyboard.type('5');
    await page.keyboard.press('Tab');
    await expect(rechner.coverage('Haftpflicht')).toBeFocused();
    await page.keyboard.press('ArrowDown'); // Pfeiltaste wählt die nächste Option: Teilkasko
    await expect(rechner.coverage('Teilkasko')).toBeChecked();
    // Eine Radio-Gruppe ist EIN Tab-Stopp. Tab springt also direkt zum Button (nicht zu "Vollkasko").
    await page.keyboard.press('Tab');
    await expect(rechner.submitButton).toBeFocused();
    await page.keyboard.press('Enter');

    await expect(rechner.resultHeading).toBeFocused();
    await expect(page.locator('.result-subtitle')).toContainText('Teilkasko');
  });

  test('meldet, wenn der Fahrzeugdienst nicht erreichbar ist (503)', async ({ page }) => {
    // Wir simulieren den Ausfall: Der Browser bekommt für /api/quotes die echte 503-Antwort des quote-service.
    await page.route('**/api/quotes', (route) =>
      route.fulfill({
        status: 503,
        contentType: 'application/problem+json',
        json: {
          status: 503,
          title: 'Fahrzeugdienst nicht erreichbar',
          detail: 'Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.',
        },
      }),
    );
    const rechner = new RechnerPage(page);
    await rechner.open();

    await rechner.fill(BERGSPORT_GT, 40, 5, 'Teilkasko');
    await rechner.submitButton.click();

    const message = 'Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.';
    await expect(page.locator('.alert-error')).toContainText(message);
    await expect(rechner.liveRegion).toHaveText(message);
    await expect(rechner.resultHeading).toHaveCount(0);
  });

  test('bietet "Erneut versuchen", wenn die Fahrzeugliste nicht lädt', async ({ page }) => {
    let failNextRequest = true;
    await page.route('**/api/vehicles', (route) => {
      if (failNextRequest) {
        failNextRequest = false;
        return route.fulfill({ status: 503, json: { status: 503 } });
      }
      return route.continue();
    });

    // exact: true = nur der sichtbare Text, nicht die (längere) Ansage in der Live-Region
    const errorText = page.getByText('Die Fahrzeugliste konnte nicht geladen werden.', { exact: true });
    await page.goto('/rechner');
    await expect(errorText).toBeVisible();

    await page.getByRole('button', { name: 'Erneut versuchen' }).click();
    await expect(page.getByRole('option', { name: BERGSPORT_GT })).toBeAttached();
    await expect(errorText).toHaveCount(0);
  });
});

test.describe('Rahmen der Seite', () => {
  for (const path of ['/rechner', '/ueber']) {
    test(`zeigt den Demo-Hinweis auf ${path}`, async ({ page }) => {
      await page.goto(path);
      await expect(page.getByText('Demo mit erfundenen Preisen. Keine echte Versicherung.')).toBeVisible();
    });
  }

  test('markiert die aktive Seite in der Navigation', async ({ page }) => {
    await page.goto('/ueber');
    const nav = page.getByRole('navigation', { name: 'Hauptnavigation' });

    await expect(nav.getByRole('link', { name: 'Über das Projekt' })).toHaveAttribute('aria-current', 'page');
    await expect(page).toHaveTitle('Über das Projekt – Musterversicherung (Demo)');
  });
});
