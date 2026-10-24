import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const MIN_DRIVING_AGE = 18;

/** Nur ganze Zahlen (type="number" erlaubt sonst auch 25,5). Leere Werte prüft "required". */
export const integerValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (value === null || value === '' || Number.isInteger(value)) {
    return null;
  }
  return { integer: true };
};

/**
 * Feldübergreifend, wie im Backend: unfallfreie Jahre <= Alter - 18.
 * Hängt an der FormGroup, weil zwei Felder zusammen geprüft werden.
 * Das Frontend prüft für schnelles Feedback. Das Backend prüft trotzdem noch einmal (Frontend ist nie sicher).
 */
export const accidentFreeYearsValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const age = group.get('driverAge')?.value;
  const years = group.get('accidentFreeYears')?.value;
  if (typeof age !== 'number' || typeof years !== 'number' || age < MIN_DRIVING_AGE) {
    return null;
  }
  const max = age - MIN_DRIVING_AGE;
  return years > max ? { accidentFreeYearsTooHigh: { max } } : null;
};
