import { FormControl, FormGroup } from '@angular/forms';
import { accidentFreeYearsValidator, integerValidator } from './quote-form.validators';

describe('accidentFreeYearsValidator', () => {
  const validate = (driverAge: number | null, accidentFreeYears: number | null) =>
    accidentFreeYearsValidator(
      new FormGroup({
        driverAge: new FormControl(driverAge),
        accidentFreeYears: new FormControl(accidentFreeYears),
      }),
    );

  it('accepts years up to age minus 18', () => {
    expect(validate(18, 0)).toBeNull();
    expect(validate(30, 12)).toBeNull();
  });

  it('rejects more years than possible and tells the maximum', () => {
    expect(validate(30, 13)).toEqual({ accidentFreeYearsTooHigh: { max: 12 } });
    expect(validate(18, 1)).toEqual({ accidentFreeYearsTooHigh: { max: 0 } });
  });

  it('ignores missing values and too young age (other validators report those)', () => {
    expect(validate(null, 5)).toBeNull();
    expect(validate(40, null)).toBeNull();
    expect(validate(17, 3)).toBeNull();
  });
});

describe('integerValidator', () => {
  it('accepts whole numbers and empty values', () => {
    expect(integerValidator(new FormControl(25))).toBeNull();
    expect(integerValidator(new FormControl(null))).toBeNull();
  });

  it('rejects decimals', () => {
    expect(integerValidator(new FormControl(25.5))).toEqual({ integer: true });
  });
});
