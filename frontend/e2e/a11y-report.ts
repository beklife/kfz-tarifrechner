import AxeBuilder from '@axe-core/playwright';
import { Page, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { A11yReport, A11yRuleInfo, A11yStateResult } from '../src/app/core/a11y-report.model';

/**
 * Sammelt die axe-Ergebnisse aller Barrierefreiheits-Tests und baut daraus public/a11y-report.json.
 * Jeder Test schreibt eine eigene Datei (Tests laufen parallel). Nach dem Lauf fügt
 * global-teardown.ts alles zu einem Bericht zusammen – aber nur, wenn ALLE Zustände geprüft wurden.
 */

export const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];
const RESULTS_DIR = path.join(__dirname, '..', 'a11y-results');
const REPORT_FILE = path.join(__dirname, '..', 'public', 'a11y-report.json');

/** Alle Zustände, die geprüft werden. Reihenfolge = Reihenfolge im Bericht. */
export const A11Y_STATES = {
  'rechner-leer': 'Rechner: leeres Formular',
  'rechner-fehler': 'Rechner: Formular mit Fehlermeldungen',
  'rechner-ergebnis': 'Rechner: mit Ergebnis und Tabelle',
  'rechner-fehler-laden': 'Rechner: Fahrzeugliste nicht ladbar',
  'rechner-mobil': 'Rechner: mobile Breite (375 px)',
  ueber: 'Über das Projekt',
  barrierefreiheit: 'Barrierefreiheit (Bericht-Seite)',
} as const;
export type A11yStateId = keyof typeof A11Y_STATES;

interface StateFile {
  result: A11yStateResult;
  axeVersion: string;
  browser: string;
}

const toRuleInfo = (rule: { id: string; help: string }): A11yRuleInfo => ({ id: rule.id, help: rule.help });

/** axe ausführen, Ergebnis speichern und prüfen: keine Verstöße. */
export async function expectNoAxeViolations(page: Page, stateId: A11yStateId): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();

  const state: StateFile = {
    result: {
      id: stateId,
      name: A11Y_STATES[stateId],
      path: new URL(page.url()).pathname,
      violations: results.violations.length,
      passes: results.passes.length,
      incomplete: results.incomplete.length,
      violationRules: results.violations.map(toRuleInfo),
      incompleteRules: results.incomplete.map(toRuleInfo),
    },
    axeVersion: results.testEngine.version,
    browser: `Chromium ${page.context().browser()?.version() ?? ''}`.trim(),
  };
  // Erst speichern, dann prüfen: Auch ein Verstoß soll ehrlich im Bericht stehen.
  mkdirSync(RESULTS_DIR, { recursive: true });
  writeFileSync(path.join(RESULTS_DIR, `${stateId}.json`), JSON.stringify(state));

  // Bei Fehlern sieht man im Testbericht direkt Regel, Beschreibung und betroffene Elemente.
  const summary = results.violations.map((violation) => ({
    rule: violation.id,
    description: violation.help,
    elements: violation.nodes.map((node) => node.target.join(' ')),
  }));
  expect(summary).toEqual([]);
}

export function clearStateResults(): void {
  rmSync(RESULTS_DIR, { recursive: true, force: true });
}

export function writeReport(): void {
  let files: string[] = [];
  try {
    files = readdirSync(RESULTS_DIR).filter((f) => f.endsWith('.json'));
  } catch {
    // Ordner fehlt: keine Barrierefreiheits-Tests gelaufen
  }
  const ids = Object.keys(A11Y_STATES) as A11yStateId[];
  if (files.length !== ids.length) {
    console.log(`a11y-report: ${files.length}/${ids.length} Zustände geprüft – Bericht bleibt unverändert.`);
    return;
  }

  const states = ids.map((id) => JSON.parse(readFileSync(path.join(RESULTS_DIR, `${id}.json`), 'utf8')) as StateFile);
  const report: A11yReport = {
    generatedAt: new Date().toISOString(),
    axeVersion: states[0].axeVersion,
    browser: states[0].browser,
    standard: WCAG_TAGS,
    commit: process.env['GITHUB_SHA'] ?? currentCommit(),
    states: states.map((s) => s.result),
  };
  writeFileSync(REPORT_FILE, JSON.stringify(report, null, 2) + '\n');
  const violations = report.states.reduce((sum, s) => sum + s.violations, 0);
  console.log(`a11y-report: ${report.states.length} Zustände, ${violations} Verstöße -> public/a11y-report.json`);
}

function currentCommit(): string | null {
  try {
    return execFileSync('git', ['rev-parse', 'HEAD'], { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
  } catch {
    return null;
  }
}
