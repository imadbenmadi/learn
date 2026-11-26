import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';

/** Errors of one signal-form field (client rules and server field errors alike), once touched. */
@Component({
  selector: 'app-field-errors',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let state = field()();
    @if (state.touched() || state.dirty()) {
      @for (e of state.errors(); track $index) {
        <small class="field__error">
          @if (e.kind === 'server') {
            <span class="badge badge--error">server</span>
          }
          {{ e.message }}
        </small>
      }
    }
  `,
})
export class FieldErrors {
  readonly field = input.required<FieldTree<string>>();
}
