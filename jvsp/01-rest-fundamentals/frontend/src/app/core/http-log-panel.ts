import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { HttpExchange, HttpLog } from './http-log';

@Component({
  selector: 'app-http-log-panel',
  imports: [JsonPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'log', '[class.log--open]': 'open()' },
  template: `
    <header class="log__bar">
      <button type="button" class="link" (click)="open.set(!open())" [attr.aria-expanded]="open()">
        {{ open() ? '▾' : '▸' }} HTTP exchanges ({{ log.exchanges().length }})
      </button>
      @if (open() && log.exchanges().length) {
        <button type="button" class="link" (click)="log.clear(); selected.set(null)">clear</button>
      }
    </header>
    @if (open()) {
      <div class="log__body">
        <ol class="log__list">
          @for (x of log.exchanges(); track x.id) {
            <li>
              <button
                type="button"
                class="log__row"
                [class.log__row--active]="selected()?.id === x.id"
                (click)="selected.set(x)"
              >
                <span class="badge" [class]="statusClass(x.status)">{{ x.status || 'ERR' }}</span>
                <span class="log__method">{{ x.method }}</span>
                <span class="log__url">{{ x.url }}</span>
                <span class="muted">{{ x.durationMs }} ms</span>
              </button>
            </li>
          } @empty {
            <li class="muted">No requests yet.</li>
          }
        </ol>
        @if (selected(); as x) {
          <div class="log__detail">
            <p>
              <strong>{{ x.method }} {{ x.url }}</strong> → {{ x.status }}
            </p>
            <p class="muted">Content-Type: {{ x.contentType ?? '(none)' }}</p>
            @if (x.location) {
              <p class="muted">Location: {{ x.location }}</p>
            }
            @if (x.requestBody) {
              <h4>Request body</h4>
              <pre>{{ x.requestBody | json }}</pre>
            }
            <h4>Response body</h4>
            <pre>{{ x.responseBody === null ? '(empty)' : (x.responseBody | json) }}</pre>
          </div>
        }
      </div>
    }
  `,
})
export class HttpLogPanel {
  protected readonly log = inject(HttpLog);
  protected readonly open = signal(false);
  protected readonly selected = signal<HttpExchange | null>(null);

  protected statusClass(status: number): string {
    if (status >= 200 && status < 300) return 'badge badge--ok';
    if (status >= 400 && status < 500) return 'badge badge--warn';
    return 'badge badge--error';
  }
}
