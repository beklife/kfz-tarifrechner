// Aufbau von public/a11y-report.json. Die E2E-Tests schreiben die Datei, die Seite /barrierefreiheit liest sie.

export interface A11yRuleInfo {
  id: string;
  help: string;
}

/** Ergebnis von axe-core für einen Zustand der Seite, z. B. "Rechner mit Fehlermeldungen". */
export interface A11yStateResult {
  id: string;
  name: string;
  path: string;
  violations: number;
  passes: number;
  incomplete: number;
  violationRules: A11yRuleInfo[];
  incompleteRules: A11yRuleInfo[];
}

export interface A11yReport {
  generatedAt: string;
  axeVersion: string;
  browser: string;
  standard: string[];
  commit: string | null;
  states: A11yStateResult[];
}
