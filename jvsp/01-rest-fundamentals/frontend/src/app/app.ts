import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { HttpLogPanel } from './core/http-log-panel';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, HttpLogPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/tasks">Task API <small>jvsp · 01 REST fundamentals</small></a>
      <nav>
        <a routerLink="/tasks" routerLinkActive="active">Tasks</a>
        <a routerLink="/inspect" routerLinkActive="active">Application context</a>
      </nav>
    </header>
    <main class="page">
      <router-outlet />
    </main>
    <app-http-log-panel />
  `,
})
export class App {}
