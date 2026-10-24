import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-demo-banner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="demo-banner" role="note">
      <strong>Demo mit erfundenen Preisen. Keine echte Versicherung.</strong>
    </p>
  `,
  styles: `
    .demo-banner {
      margin: 0;
      padding: var(--space-2) var(--space-3);
      background: var(--color-banner-bg);
      color: var(--color-banner-text);
      text-align: center;
      border-bottom: 2px solid var(--color-banner-text);
    }
  `,
})
export class DemoBanner {}
