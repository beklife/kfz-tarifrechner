import { Locator, Page, expect } from '@playwright/test';

/**
 * "Page Object": kapselt, wie man die Rechner-Seite bedient.
 * Die Tests lesen sich dadurch wie eine Beschreibung, und Selektoren stehen nur an einer Stelle.
 * Wir suchen Elemente wie ein Mensch: über Rolle und sichtbaren Namen (getByRole, getByLabel).
 */
export class RechnerPage {
  readonly vehicle: Locator;
  readonly driverAge: Locator;
  readonly accidentFreeYears: Locator;
  readonly submitButton: Locator;
  readonly resultHeading: Locator;
  readonly liveRegion: Locator;

  constructor(private readonly page: Page) {
    this.vehicle = page.getByLabel('Fahrzeug', { exact: true });
    this.driverAge = page.getByLabel('Alter der Fahrerin / des Fahrers');
    this.accidentFreeYears = page.getByLabel('Unfallfreie Jahre');
    this.submitButton = page.getByRole('button', { name: 'Preis berechnen' });
    this.resultHeading = page.getByRole('heading', { name: 'Ihr Ergebnis' });
    this.liveRegion = page.getByRole('status');
  }

  async open(): Promise<void> {
    await this.page.goto('/rechner');
    await expect(this.vehicle.locator('option')).not.toHaveCount(1);
  }

  coverage(label: 'Haftpflicht' | 'Teilkasko' | 'Vollkasko'): Locator {
    return this.page.getByRole('radio', { name: label });
  }

  price(label: 'Jahrespreis' | 'Monatspreis'): Locator {
    return this.page.locator('.price').filter({ hasText: label }).locator('dd');
  }

  async fill(vehicleLabel: string, age: number, years: number, coverage: 'Haftpflicht' | 'Teilkasko' | 'Vollkasko') {
    await this.vehicle.selectOption({ label: vehicleLabel });
    await this.driverAge.fill(String(age));
    await this.accidentFreeYears.fill(String(years));
    await this.coverage(coverage).check();
  }
}
