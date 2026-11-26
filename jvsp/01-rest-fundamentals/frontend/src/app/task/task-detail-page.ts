import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Problem, toProblem } from '../shared/problem';
import { ProblemAlert } from '../shared/problem-alert';
import { TaskApi } from './task-api';
import { TaskForm, TaskFormValue, toNullable } from './task-form';
import { TASK_STATUSES, TaskStatus, allowedTransitions, label } from './task.model';

@Component({
  selector: 'app-task-detail-page',
  imports: [DatePipe, RouterLink, ProblemAlert, TaskForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-detail-page.html',
})
export class TaskDetailPage {
  private readonly api = inject(TaskApi);
  private readonly router = inject(Router);

  /** Bound from the :id route parameter (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly label = label;
  protected readonly task = this.api.taskResource(this.id);
  protected readonly loadProblem = computed(() => {
    const error = this.task.error();
    return error ? toProblem(error) : null;
  });
  protected readonly actionProblem = signal<Problem | null>(null);
  protected readonly confirmingDelete = signal(false);

  protected readonly allowed = computed(() =>
    this.task.hasValue() ? allowedTransitions(this.task.value().status) : [],
  );
  /** The statuses the backend will refuse with 409, offered on purpose. */
  protected readonly forbidden = computed(() => {
    if (!this.task.hasValue()) return [];
    const current = this.task.value().status;
    return TASK_STATUSES.filter((s) => s !== current && !this.allowed().includes(s));
  });

  protected readonly formValue = computed<TaskFormValue | null>(() => {
    if (!this.task.hasValue()) return null;
    const t = this.task.value();
    return {
      title: t.title,
      description: t.description ?? '',
      priority: t.priority,
      dueDate: t.dueDate,
    };
  });

  protected readonly saveTask = async (value: TaskFormValue): Promise<void> => {
    const updated = await firstValueFrom(
      this.api.update(this.id(), {
        title: value.title,
        description: toNullable(value.description),
        priority: value.priority === '' ? null : value.priority,
        dueDate: toNullable(value.dueDate),
      }),
    );
    this.task.set(updated);
  };

  protected async changeStatus(status: TaskStatus): Promise<void> {
    await this.run(async () =>
      this.task.set(await firstValueFrom(this.api.changeStatus(this.id(), status))),
    );
  }

  protected async delete(): Promise<void> {
    await this.run(async () => {
      await firstValueFrom(this.api.delete(this.id()));
      await this.router.navigate(['/tasks']);
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.actionProblem.set(null);
    try {
      await action();
    } catch (error) {
      this.actionProblem.set(toProblem(error));
    } finally {
      this.confirmingDelete.set(false);
    }
  }
}
