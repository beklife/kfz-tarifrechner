import { clearStateResults } from './a11y-report';

// Vor dem Lauf: alte Einzel-Ergebnisse löschen, damit der Bericht nur den aktuellen Lauf zeigt.
export default function globalSetup(): void {
  clearStateResults();
}
