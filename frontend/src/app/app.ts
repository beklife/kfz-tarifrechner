import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DemoBanner } from './shared/demo-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DemoBanner],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
