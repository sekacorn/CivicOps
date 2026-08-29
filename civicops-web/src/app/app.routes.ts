import { Routes } from '@angular/router';
import { WelcomeComponent } from './core/welcome.component';

export const routes: Routes = [
  { path: '', component: WelcomeComponent, title: 'CivicOps' },
  { path: '**', redirectTo: '' }
];
