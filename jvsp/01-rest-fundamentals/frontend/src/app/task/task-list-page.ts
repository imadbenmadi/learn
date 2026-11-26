import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ProblemAlert } from '../shared/problem-alert';
import { toProblem } from '../shared/problem';
import { TaskApi } from './task-api';
import { TaskForm, TaskFormValue, toNullable } from './task-form';
import { TASK_STATUSES, Task, TaskPriority, TaskStatus, label } from './task.model';

@Component({
  selector: 'app-task-list-page',
  imports: [DatePipe, RouterLink, ProblemAlert, TaskForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-list-page.html',
})
export class TaskListPage {
  private readonly api = inject(TaskApi);

  protected readonly statuses = TASK_STATUSES;
  protected readonly label = label;
  protected readonly status = signal<TaskStatus | null>(null);
  protected readonly tasks = this.api.listResource(this.status);
  protected readonly lastCreated = signal<Task | null>(null);

  protected readonly createTask = async (value: TaskFormValue): Promise<void> => {
    const created = await firstValueFrom(
      this.api.create({
        title: value.title,
        description: toNullable(value.description),
        priority: value.priority === '' ? null : value.priority,
        dueDate: toNullable(value.dueDate),
      }),
    );
    this.lastCreated.set(created);
    this.tasks.reload();
  };

  protected readonly listProblem = computed(() => {
    const error = this.tasks.error();
    return error ? toProblem(error) : null;
  });

  protected priorityClass(priority: TaskPriority): string {
    return `badge badge--prio-${priority.toLowerCase()}`;
  }
}
