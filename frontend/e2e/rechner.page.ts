import { Locator, Page, expect } from '@playwright/test';

/**
 * "Page Object": kapselt, wie man die Rechner-Seite bedient.
 * Die Tests lesen sich dadurch wie eine Beschreibung, und Selektoren stehen nur an einer Stelle.
 * Wir suchen Elemente wie ein Mensch bzw. Screenreader: über Rolle und Namen (getByRole).
 */
export class RechnerPage {
  readonly vehicle: Locator;
  readonly driverAge: Locator;
  readonly accidentFreeYears: Locator;
  readonly submitButton: Locator;
  readonly resultHeading: Locator;
  readonly liveRegion: Locator;

  constructor(private readonly page: Page) {
    // getByRole + name nutzt den "accessible name", also das, was ein Screenreader vorliest.
    // Der Stern (*) hat aria-hidden und gehört deshalb NICHT zum Namen: "Fahrzeug", nicht "Fahrzeug *".
    this.vehicle = page.getByRole('combobox', { name: 'Fahrzeug', exact: true });
    this.driverAge = page.getByRole('spinbutton', { name: 'Alter der Fahrerin / des Fahrers', exact: true });
    this.accidentFreeYears = page.getByRole('spinbutton', { name: 'Unfallfreie Jahre', exact: true });
    this.submitButton = page.getByRole('button', { name: 'Preis berechnen' });
    this.resultHeading = page.getByRole('heading', { name: 'Ihr Ergebnis' });
    this.liveRegion = page.getByRole('status');
  }

  async open(): Promise<void> {
    await this.page.goto('/rechner');
    // Warten, bis die Fahrzeuge geladen sind: Option 2 = erstes echtes Fahrzeug (Option 1 = "Bitte auswählen").
    // Nicht "not.toHaveCount(1)": Das stimmt auch bei 0 Optionen, also bevor Angular überhaupt etwas gezeichnet hat.
    await expect(this.vehicle.locator('option').nth(1)).toBeAttached();
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
