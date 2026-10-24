import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuoteResponse, Vehicle } from '../../core/api.models';
import { Rechner } from './rechner';

registerLocaleData(localeDe);

const VEHICLES: Vehicle[] = [
  { id: 1, manufacturer: 'Autowerk Nord', model: 'Kompakt K1', riskClass: 1 },
  { id: 4, manufacturer: 'Rheinmobil', model: 'Touring 500', riskClass: 3 },
];

const QUOTE: QuoteResponse = {
  vehicleName: 'Rheinmobil Touring 500',
  coverage: 'TEILKASKO',
  yearlyPrice: 1456.88,
  monthlyPrice: 121.41,
  breakdown: [
    { step: 1, description: 'Grundpreis für Risikoklasse 3', factorOrAmount: 430, subtotal: 430 },
    { step: 2, description: 'Altersfaktor (30–64 Jahre)', factorOrAmount: 1, subtotal: 430 },
  ],
};

describe('Rechner', () => {
  let fixture: ComponentFixture<Rechner>;
  let http: HttpTestingController;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Rechner],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'de-DE' }],
    }).compileComponents();

    fixture = TestBed.createComponent(Rechner);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function loadVehicles(): Promise<void> {
    http.expectOne('/api/vehicles').flush(VEHICLES);
    await fixture.whenStable();
  }

  async function fillValidForm(): Promise<void> {
    const select = element.querySelector<HTMLSelectElement>('#vehicleId')!;
    select.value = select.options[2].value; // "Rheinmobil Touring 500"
    select.dispatchEvent(new Event('change'));
    setNumber('#driverAge', 40);
    setNumber('#accidentFreeYears', 5);
    element.querySelector<HTMLInputElement>('#coverage-TEILKASKO')!.click();
    await fixture.whenStable();
  }

  function setNumber(selector: string, value: number): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = String(value);
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('loads vehicles into the select', async () => {
    await loadVehicles();

    const options = Array.from(element.querySelectorAll('#vehicleId option')).map((o) => o.textContent?.trim());
    expect(options).toEqual([
      'Bitte auswählen',
      'Autowerk Nord Kompakt K1 (Risikoklasse 1)',
      'Rheinmobil Touring 500 (Risikoklasse 3)',
    ]);
  });

  it('shows an error with retry button when vehicles cannot be loaded', async () => {
    http.expectOne('/api/vehicles').flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(element.textContent).toContain('Die Fahrzeugliste konnte nicht geladen werden.');
    element.querySelector<HTMLButtonElement>('.alert button')!.click();
    await loadVehicles();
    expect(element.querySelectorAll('#vehicleId option')).toHaveLength(3);
  });

  it('shows no errors before touch or submit', async () => {
    await loadVehicles();

    expect(element.querySelectorAll('.error-message')).toHaveLength(0);
  });

  it('shows all errors after submit, linked with aria-describedby, and sends nothing', async () => {
    await loadVehicles();
    await submit();

    expect(element.querySelectorAll('.error-message')).toHaveLength(4);
    const age = element.querySelector('#driverAge')!;
    expect(age.getAttribute('aria-invalid')).toBe('true');
    expect(age.getAttribute('aria-describedby')).toBe('driverAge-hint driverAge-error');
    expect(element.querySelector('#driverAge-error')?.textContent).toContain('Bitte das Alter angeben.');
    http.expectNone('/api/quotes');
  });

  it('shows the cross-field error at the accident-free years field', async () => {
    await loadVehicles();
    await fillValidForm();
    setNumber('#driverAge', 30);
    setNumber('#accidentFreeYears', 13);
    await submit();

    expect(element.querySelector('#accidentFreeYears-error')?.textContent).toContain(
      'höchstens 12 unfallfreie Jahre',
    );
    http.expectNone('/api/quotes');
  });

  it('sends the request and shows the result in German format', async () => {
    await loadVehicles();
    await fillValidForm();
    await submit();

    const request = http.expectOne('/api/quotes');
    expect(request.request.body).toEqual({ vehicleId: 4, driverAge: 40, accidentFreeYears: 5, coverage: 'TEILKASKO' });
    request.flush(QUOTE);
    await fixture.whenStable();

    const prices = Array.from(element.querySelectorAll('.price dd')).map((dd) => dd.textContent?.trim());
    // Angular nutzt ein geschütztes Leerzeichen (\u00a0) vor dem €-Zeichen.
    expect(prices).toEqual(['1.456,88 €', '121,41 €']);
    expect(element.querySelector('caption')?.textContent).toContain('So setzt sich der Jahrespreis zusammen');
    expect(element.querySelectorAll('tbody th[scope="row"]')).toHaveLength(2);
    expect(element.querySelector('[aria-live="polite"]')?.textContent).toContain('1.456,88');
  });

  it('moves focus to the result heading', async () => {
    await loadVehicles();
    await fillValidForm();
    await submit();
    http.expectOne('/api/quotes').flush(QUOTE);
    await fixture.whenStable();

    expect(document.activeElement?.id).toBe('result-heading');
  });

  it('shows the German message when the vehicle service is down', async () => {
    await loadVehicles();
    await fillValidForm();
    await submit();
    http.expectOne('/api/quotes').flush(
      { status: 503, detail: 'Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.' },
      { status: 503, statusText: 'Service Unavailable' },
    );
    await fixture.whenStable();

    expect(element.querySelector('.alert-error')?.textContent).toContain(
      'Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.',
    );
    expect(element.querySelector('.result')).toBeNull();
  });

  it('puts server field errors (400) at the matching field', async () => {
    await loadVehicles();
    await fillValidForm();
    await submit();
    http.expectOne('/api/quotes').flush(
      { status: 400, errors: [{ field: 'vehicleId', message: 'Das Fahrzeug mit der ID 4 gibt es nicht.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(element.querySelector('#vehicleId-error')?.textContent).toContain('Das Fahrzeug mit der ID 4 gibt es nicht.');
  });
});
