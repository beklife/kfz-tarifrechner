import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { GITHUB_URL } from '../../app-info';
import { A11yReport, A11yRuleInfo } from '../../core/a11y-report.model';

const WCAG_LABELS: Record<string, string> = {
  wcag2a: 'WCAG 2.0 A',
  wcag2aa: 'WCAG 2.0 AA',
  wcag21a: 'WCAG 2.1 A',
  wcag21aa: 'WCAG 2.1 AA',
};

@Component({
  selector: 'app-barrierefreiheit',
  imports: [DatePipe],
  templateUrl: './barrierefreiheit.html',
  styleUrl: './barrierefreiheit.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Barrierefreiheit {
  // httpResource lädt die Datei und stellt Wert, Ladezustand und Fehler als Signals bereit.
  // Die Datei schreiben die E2E-Tests (in der CI bei jedem Push neu).
  protected readonly report = httpResource<A11yReport>(() => '/a11y-report.json');

  /** Bericht plus Summen über alle Zustände – oder null, solange nichts geladen ist. */
  protected readonly view = computed(() => {
    const data = this.report.hasValue() ? this.report.value() : undefined;
    if (!data) {
      return null;
    }
    const sum = (key: 'violations' | 'passes' | 'incomplete') =>
      data.states.reduce((total, state) => total + state[key], 0);
    const totals = { states: data.states.length, violations: sum('violations'), passes: sum('passes'), incomplete: sum('incomplete') };
    return { data, totals };
  });

  /** Regeln, die axe nicht sicher automatisch entscheiden kann – ohne Doppelte. */
  protected readonly manualRules = computed<A11yRuleInfo[]>(() => {
    const report = this.report.hasValue() ? this.report.value() : undefined;
    const byId = new Map<string, A11yRuleInfo>();
    report?.states.flatMap((state) => state.incompleteRules).forEach((rule) => byId.set(rule.id, rule));
    return [...byId.values()];
  });

  protected standardLabel(tags: string[]): string {
    return tags.map((tag) => WCAG_LABELS[tag] ?? tag).join(', ');
  }

  protected commitUrl(commit: string): string {
    return `${GITHUB_URL}/commit/${commit}`;
  }
}
