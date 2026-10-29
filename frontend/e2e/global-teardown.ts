import { writeReport } from './a11y-report';

// Nach dem Lauf: Einzel-Ergebnisse zu public/a11y-report.json zusammenfügen.
export default function globalTeardown(): void {
  writeReport();
}
