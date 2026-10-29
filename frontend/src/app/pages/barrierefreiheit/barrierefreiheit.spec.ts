import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { A11yReport } from '../../core/a11y-report.model';
import { Barrierefreiheit } from './barrierefreiheit';

registerLocaleData(localeDe);

const REPORT: A11yReport = {
  generatedAt: '2026-10-29T10:15:00.000Z',
  axeVersion: '4.11.0',
  browser: 'Chromium 140',
  standard: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
  commit: 'abcdef1234567890',
  states: [
    {
      id: 'rechner-leer', name: 'Rechner: leeres Formular', path: '/rechner',
      violations: 0, passes: 30, incomplete: 1, violationRules: [],
      incompleteRules: [{ id: 'color-contrast', help: 'Elements must meet minimum color contrast ratio thresholds' }],
    },
    {
      id: 'ueber', name: 'Über das Projekt', path: '/ueber',
      violations: 0, passes: 25, incomplete: 1, violationRules: [],
      incompleteRules: [{ id: 'color-contrast', help: 'Elements must meet minimum color contrast ratio thresholds' }],
    },
  ],
};

describe('Barrierefreiheit', () => {
  let fixture: ComponentFixture<Barrierefreiheit>;
  let http: HttpTestingController;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Barrierefreiheit],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'de-DE' }],
    }).compileComponents();
    fixture = TestBed.createComponent(Barrierefreiheit);
    http = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    // Nicht auf whenStable() warten: httpResource zählt die offene Anfrage als "noch nicht fertig".
    // Stabil wird es erst, wenn der Test in respond() antwortet.
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function respond(body: A11yReport | null, status = 200): Promise<void> {
    const request = http.expectOne('/a11y-report.json');
    if (status === 200) {
      request.flush(body);
    } else {
      request.flush(null, { status, statusText: 'Not Found' });
    }
    await fixture.whenStable();
  }

  it('shows the summary tiles with totals over all states', async () => {
    await respond(REPORT);

    const tiles = Array.from(element.querySelectorAll('.tile-value')).map((t) => t.textContent?.trim());
    // Verstöße, Zustände, bestandene Prüfungen (30 + 25), Hinweise (1 + 1)
    expect(tiles).toEqual(['0', '2', '55', '2']);
    expect(element.textContent).toContain('keine gefunden');
  });

  it('shows one table row per state with caption and row headers', async () => {
    await respond(REPORT);

    expect(element.querySelector('caption')?.textContent).toContain('Ergebnis je Seiten-Zustand');
    const rowHeaders = Array.from(element.querySelectorAll('tbody th[scope="row"]')).map((th) => th.textContent?.trim());
    expect(rowHeaders).toEqual(['Rechner: leeres Formular', 'Über das Projekt']);
  });

  it('shows metadata: German date, tool and a link to the commit', async () => {
    await respond(REPORT);

    expect(element.textContent).toContain('29. Oktober 2026');
    expect(element.textContent).toContain('axe-core 4.11.0');
    expect(element.textContent).toContain('WCAG 2.1 AA');
    const link = element.querySelector<HTMLAnchorElement>('.meta a');
    expect(link?.href).toBe('https://github.com/beklife/kfz-tarifrechner/commit/abcdef1234567890');
    expect(link?.textContent).toContain('abcdef1');
  });

  it('lists each rule for manual checking only once', async () => {
    await respond(REPORT);

    expect(element.querySelectorAll('.manual li')).toHaveLength(1);
  });

  it('shows an error with reload button when the report is missing', async () => {
    await respond(null, 404);

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Der Testbericht konnte nicht geladen werden.');
    element.querySelector<HTMLButtonElement>('.alert button')!.click();
    fixture.detectChanges();
    await respond(REPORT);
    expect(element.querySelectorAll('tbody tr')).toHaveLength(2);
  });
});
