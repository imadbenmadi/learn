import { ChangeDetectionStrategy, Component, input, linkedSignal, signal } from '@angular/core';
import {
  FieldTree,
  FormField,
  ValidationError,
  applyWhen,
  form,
  maxLength,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
import { FieldErrors } from '../shared/field-errors';
import { Problem, toProblem } from '../shared/problem';
import { ProblemAlert } from '../shared/problem-alert';
import { TASK_PRIORITIES, TaskPriority, label } from './task.model';

/** Everything as strings, the way native inputs hold it. '' means "not given" (null on the wire). */
export interface TaskFormValue {
  readonly title: string;
  readonly description: string;
  readonly priority: TaskPriority | '';
  readonly dueDate: string;
}

export const EMPTY_TASK_FORM: TaskFormValue = {
  title: '',
  description: '',
  priority: '',
  dueDate: '',
};

type FieldName = keyof TaskFormValue;

/**
 * Create and edit form. The client-side rules repeat the Bean Validation constraints on
 * CreateTaskRequest / UpdateTaskRequest; switch them off to send invalid input and see Spring
 * reject it with a 400 ProblemDetail, whose field errors are then shown on the matching inputs.
 */
@Component({
  selector: 'app-task-form',
  imports: [FormField, FieldErrors, ProblemAlert],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-form.html',
})
export class TaskForm {
  /** 'create' matches CreateTaskRequest (priority/due date optional), 'edit' UpdateTaskRequest. */
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<TaskFormValue>(EMPTY_TASK_FORM);
  /** Sends the request. Rejections are turned into a Problem and shown on the form. */
  readonly action = input.required<(value: TaskFormValue) => Promise<unknown>>();

  protected readonly priorities = TASK_PRIORITIES;
  protected readonly label = label;
  protected readonly clientValidation = signal(true);
  protected readonly problem = signal<Problem | null>(null);

  // linkedSignal: a writable copy that resets whenever the parent passes a new initial value.
  protected readonly model = linkedSignal(() => this.initial());

  protected readonly taskForm = form(this.model, (path) => {
    applyWhen(
      path,
      () => this.clientValidation(),
      (p) => {
        required(p.title, { message: 'must not be blank' });
        maxLength(p.title, 200, { message: 'size must be between 0 and 200' });
        maxLength(p.description, 2000, { message: 'size must be between 0 and 2000' });
        required(p.priority, { message: 'must not be null', when: () => this.mode() === 'edit' });
        required(p.dueDate, { message: 'must not be null', when: () => this.mode() === 'edit' });
        validate(p.dueDate, ({ value }) =>
          this.mode() === 'create' && value() !== '' && value() < today()
            ? { kind: 'futureOrPresent', message: 'must be a date in the present or in the future' }
            : undefined,
        );
      },
    );
  });

  protected async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    this.problem.set(null);
    await submit(this.taskForm, async () => {
      try {
        await this.action()(this.model());
        if (this.mode() === 'create') {
          this.model.set(EMPTY_TASK_FORM);
          this.taskForm().reset();
        }
        return undefined;
      } catch (error) {
        const problem = toProblem(error);
        this.problem.set(problem);
        return problem.errors.flatMap((violation): ValidationError.WithFieldTree[] => {
          const field = this.fieldFor(violation.field);
          return field ? [{ fieldTree: field, kind: 'server', message: violation.message }] : [];
        });
      }
    });
  }

  private fieldFor(name: string): FieldTree<string> | null {
    const fields: Record<FieldName, FieldTree<string>> = {
      title: this.taskForm.title,
      description: this.taskForm.description,
      priority: this.taskForm.priority,
      dueDate: this.taskForm.dueDate,
    };
    return name in fields ? fields[name as FieldName] : null;
  }
}

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function toNullable(value: string): string | null {
  return value.trim() === '' ? null : value;
}
