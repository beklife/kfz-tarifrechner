import { CurrencyPipe, DecimalPipe, formatCurrency } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  LOCALE_ID,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Coverage, ProblemDetail, QuoteResponse, Vehicle } from '../../core/api.models';
import { QuoteApi } from '../../core/quote-api';
import { VehicleApi } from '../../core/vehicle-api';
import { accidentFreeYearsValidator, integerValidator } from './quote-form.validators';

type FieldName = 'vehicleId' | 'driverAge' | 'accidentFreeYears' | 'coverage';
type LoadState = 'loading' | 'loaded' | 'error';
type SubmitState = 'idle' | 'loading' | 'success' | 'error';

// Reihenfolge wie im Formular. Wird gebraucht, um das erste fehlerhafte Feld zu fokussieren.
const FIELD_ORDER: FieldName[] = ['vehicleId', 'driverAge', 'accidentFreeYears', 'coverage'];

const REQUIRED_MESSAGES: Record<FieldName, string> = {
  vehicleId: 'Bitte ein Fahrzeug auswählen.',
  driverAge: 'Bitte das Alter angeben.',
  accidentFreeYears: 'Bitte die unfallfreien Jahre angeben.',
  coverage: 'Bitte eine Versicherungsart auswählen.',
};

const HINTS: Partial<Record<FieldName, string>> = {
  driverAge: 'Ganze Zahl von 18 bis 99.',
  accidentFreeYears: 'Ganze Zahl von 0 bis 50. Höchstens Alter minus 18.',
};

export const COVERAGE_OPTIONS: { value: Coverage; label: string; hint: string }[] = [
  { value: 'HAFTPFLICHT', label: 'Haftpflicht', hint: 'Schäden, die Sie anderen verursachen.' },
  { value: 'TEILKASKO', label: 'Teilkasko', hint: 'Zusätzlich z. B. Diebstahl, Glasbruch, Sturm.' },
  { value: 'VOLLKASKO', label: 'Vollkasko', hint: 'Zusätzlich selbst verursachte Schäden am eigenen Auto.' },
];

export const VEHICLE_SERVICE_DOWN_MESSAGE =
  'Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.';
const GENERIC_ERROR_MESSAGE = 'Der Preis konnte gerade nicht berechnet werden. Bitte später erneut versuchen.';

@Component({
  selector: 'app-rechner',
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './rechner.html',
  styleUrl: './rechner.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Rechner {
  private readonly vehicleApi = inject(VehicleApi);
  private readonly quoteApi = inject(QuoteApi);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);
  private readonly locale = inject(LOCALE_ID);

  protected readonly coverageOptions = COVERAGE_OPTIONS;
  protected readonly hints = HINTS;

  // --- Zustand als Signals ---
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly vehiclesState = signal<LoadState>('loading');
  protected readonly submitState = signal<SubmitState>('idle');
  protected readonly quote = signal<QuoteResponse | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Wurde "Preis berechnen" schon einmal geklickt? Dann zeigen wir alle Fehler, auch ohne "touched". */
  protected readonly submitted = signal(false);

  /** Text für die aria-live-Region. Screenreader lesen jede Änderung vor. */
  protected readonly liveMessage = computed(() => {
    if (this.vehiclesState() === 'error') {
      return 'Die Fahrzeugliste konnte nicht geladen werden. ' + VEHICLE_SERVICE_DOWN_MESSAGE;
    }
    switch (this.submitState()) {
      case 'loading':
        return 'Preis wird berechnet …';
      case 'error':
        return this.errorMessage() ?? '';
      case 'success': {
        const quote = this.quote();
        return quote
          ? `Ergebnis: ${this.formatEuro(quote.yearlyPrice)} pro Jahr, ${this.formatEuro(quote.monthlyPrice)} pro Monat.`
          : '';
      }
      default:
        return '';
    }
  });

  private readonly resultHeading = viewChild<ElementRef<HTMLElement>>('resultHeading');

  protected readonly form = new FormGroup(
    {
      vehicleId: new FormControl<number | null>(null, Validators.required),
      driverAge: new FormControl<number | null>(null, [
        Validators.required,
        integerValidator,
        Validators.min(18),
        Validators.max(99),
      ]),
      accidentFreeYears: new FormControl<number | null>(null, [
        Validators.required,
        integerValidator,
        Validators.min(0),
        Validators.max(50),
      ]),
      coverage: new FormControl<Coverage | null>(null, Validators.required),
    },
    { validators: accidentFreeYearsValidator },
  );

  constructor() {
    this.loadVehicles();
  }

  protected loadVehicles(): void {
    this.vehiclesState.set('loading');
    this.vehicleApi
      .getVehicles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (vehicles) => {
          this.vehicles.set(vehicles);
          this.vehiclesState.set('loaded');
        },
        error: () => this.vehiclesState.set('error'),
      });
  }

  protected onSubmit(): void {
    this.submitted.set(true);
    if (this.form.invalid) {
      this.focusFirstInvalidField();
      return;
    }
    if (this.submitState() === 'loading') {
      return; // Doppelklick ignorieren
    }

    const { vehicleId, driverAge, accidentFreeYears, coverage } = this.form.getRawValue();
    this.submitState.set('loading');
    this.errorMessage.set(null);
    this.quote.set(null);

    this.quoteApi
      // Nach der Prüfung oben sind alle Werte gesetzt, darum "!" (nicht null).
      .createQuote({ vehicleId: vehicleId!, driverAge: driverAge!, accidentFreeYears: accidentFreeYears!, coverage: coverage! })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (quote) => {
          this.quote.set(quote);
          this.submitState.set('success');
          // Erst nach dem Rendern existiert die Überschrift. Dann Fokus dorthin.
          afterNextRender(() => this.resultHeading()?.nativeElement.focus(), { injector: this.injector });
        },
        error: (error: HttpErrorResponse) => this.handleQuoteError(error),
      });
  }

  /** Die Fehlermeldung für ein Feld – oder null, wenn (noch) keine gezeigt werden soll. */
  protected errorFor(field: FieldName): string | null {
    const control = this.form.controls[field];
    if (!control.touched && !this.submitted()) {
      return null;
    }
    const errors = control.errors;
    if (errors?.['required']) return REQUIRED_MESSAGES[field];
    if (errors?.['integer']) return 'Bitte eine ganze Zahl eingeben.';
    if (errors?.['min'] && field === 'driverAge') return 'Das Alter muss mindestens 18 sein.';
    if (errors?.['max'] && field === 'driverAge') return 'Das Alter darf höchstens 99 sein.';
    if (errors?.['min'] && field === 'accidentFreeYears') return 'Die unfallfreien Jahre dürfen nicht negativ sein.';
    if (errors?.['max'] && field === 'accidentFreeYears') return 'Die unfallfreien Jahre dürfen höchstens 50 sein.';
    if (errors?.['server']) return errors['server'];

    const tooHigh = this.form.errors?.['accidentFreeYearsTooHigh'];
    if (field === 'accidentFreeYears' && tooHigh) {
      return `Bei diesem Alter sind höchstens ${tooHigh.max} unfallfreie Jahre möglich (Alter minus 18).`;
    }
    return null;
  }

  /** Verbindet Eingabefeld mit Hinweis und Fehlermeldung (aria-describedby). */
  protected describedBy(field: FieldName, error: string | null): string | null {
    const ids = [this.hints[field] ? `${field}-hint` : null, error ? `${field}-error` : null].filter(Boolean);
    return ids.length > 0 ? ids.join(' ') : null;
  }

  protected coverageLabel(coverage: Coverage): string {
    return COVERAGE_OPTIONS.find((option) => option.value === coverage)?.label ?? coverage;
  }

  /** Schritt 1 ist ein Betrag (Grundpreis), alle anderen Schritte sind Faktoren. */
  protected isAmountStep(step: number): boolean {
    return step === 1;
  }

  private handleQuoteError(error: HttpErrorResponse): void {
    const problem = error.error as ProblemDetail | null;

    // 400 mit Feldfehlern: Meldungen vom Server direkt an die Felder hängen.
    if (error.status === 400 && problem?.errors?.length) {
      for (const fieldError of problem.errors) {
        this.form.get(fieldError.field)?.setErrors({ server: fieldError.message });
      }
      this.submitState.set('idle');
      this.focusFirstInvalidField();
      return;
    }

    // 503 = vehicle-service weg. 502/504/0 = nginx oder Netzwerk erreicht den quote-service nicht.
    const unavailable = [0, 502, 503, 504].includes(error.status);
    this.errorMessage.set(unavailable ? (problem?.detail ?? VEHICLE_SERVICE_DOWN_MESSAGE) : GENERIC_ERROR_MESSAGE);
    this.submitState.set('error');
  }

  private focusFirstInvalidField(): void {
    const firstInvalid = FIELD_ORDER.find((field) => this.errorFor(field) !== null);
    if (!firstInvalid) {
      return;
    }
    // Bei der Radio-Gruppe den ersten Radio-Button fokussieren.
    const id = firstInvalid === 'coverage' ? `coverage-${COVERAGE_OPTIONS[0].value}` : firstInvalid;
    afterNextRender(() => document.getElementById(id)?.focus(), { injector: this.injector });
  }

  private formatEuro(value: number): string {
    return formatCurrency(value, this.locale, '€', 'EUR');
  }
}
