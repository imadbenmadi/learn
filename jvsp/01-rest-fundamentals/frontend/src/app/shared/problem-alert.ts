import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { Problem } from './problem';

/** Renders a ProblemDetail response member by member, so its RFC 9457 structure stays visible. */
@Component({
  selector: 'app-problem-alert',
  imports: [JsonPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let p = problem();
    <div class="problem" role="alert">
      <div class="problem__head">
        @if (p.status) {
          <span class="badge badge--error">{{ p.status }}</span>
        }
        <strong>{{ p.title }}</strong>
        @if (p.fromServer) {
          <code class="problem__type">{{ p.type }}</code>
        }
      </div>
      @if (p.detail) {
        <p class="problem__detail">{{ p.detail }}</p>
      }
      @if (p.errors.length) {
        <ul class="problem__errors">
          @for (e of p.errors; track e.field + e.message) {
            <li>
              <code>{{ e.field }}</code> {{ e.message }}
            </li>
          }
        </ul>
      }
      @if (extensions().length) {
        <dl class="problem__ext">
          @for (entry of extensions(); track entry[0]) {
            <dt>{{ entry[0] }}</dt>
            <dd>
              <code>{{ entry[1] | json }}</code>
            </dd>
          }
        </dl>
      }
    </div>
  `,
})
export class ProblemAlert {
  readonly problem = input.required<Problem>();
  protected readonly extensions = computed(() => Object.entries(this.problem().extensions));
}
