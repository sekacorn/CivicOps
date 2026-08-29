import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'cop-root',
  imports: [RouterLink, RouterOutlet],
  template: `<header><a routerLink="/">CivicOps</a><span>Open-source operations platform for nonprofits and community organizations.</span></header><main><router-outlet /></main>`
})
export class AppComponent {}
