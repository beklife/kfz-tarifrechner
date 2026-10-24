import { ChangeDetectionStrategy, Component } from '@angular/core';
import { GITHUB_URL } from '../../app-info';

@Component({
  selector: 'app-ueber',
  templateUrl: './ueber.html',
  styleUrl: './ueber.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ueber {
  protected readonly githubUrl = GITHUB_URL;
}
