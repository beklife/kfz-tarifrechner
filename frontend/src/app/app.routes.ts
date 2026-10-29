import { Routes } from '@angular/router';

// "title" setzt den Tab-Titel. Wichtig für Screenreader: Er wird beim Seitenwechsel vorgelesen.
// loadComponent lädt jede Seite erst, wenn sie gebraucht wird (Lazy Loading).
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'rechner' },
  {
    path: 'rechner',
    title: 'Tarifrechner – Musterversicherung (Demo)',
    loadComponent: () => import('./pages/rechner/rechner').then((m) => m.Rechner),
  },
  {
    path: 'ueber',
    title: 'Über das Projekt – Musterversicherung (Demo)',
    loadComponent: () => import('./pages/ueber/ueber').then((m) => m.Ueber),
  },
  {
    path: 'barrierefreiheit',
    title: 'Barrierefreiheit – Musterversicherung (Demo)',
    loadComponent: () => import('./pages/barrierefreiheit/barrierefreiheit').then((m) => m.Barrierefreiheit),
  },
  { path: '**', redirectTo: 'rechner' },
];
