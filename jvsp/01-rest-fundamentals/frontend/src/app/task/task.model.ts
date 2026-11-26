// TypeScript mirror of the backend's API contract (the records in dev.jvsp.taskapi.task).
// Dates travel as ISO strings: LocalDate as "2026-12-01", Instant as "2026-11-26T10:15:30.123Z".

export const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'] as const;
export type TaskStatus = (typeof TASK_STATUSES)[number];

export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const;
export type TaskPriority = (typeof TASK_PRIORITIES)[number];

/** TaskResponse */
export interface Task {
  readonly id: string;
  readonly title: string;
  readonly description: string | null;
  readonly priority: TaskPriority;
  readonly status: TaskStatus;
  readonly dueDate: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** CreateTaskRequest: null priority and dueDate get defaults in TaskService. */
export interface CreateTaskRequest {
  readonly title: string;
  readonly description: string | null;
  readonly priority: TaskPriority | null;
  readonly dueDate: string | null;
}

/**
 * UpdateTaskRequest: PUT replaces every editable field. priority and dueDate are @NotNull on the
 * server; null is allowed here only so the UI can send it and show Spring's 400.
 */
export interface UpdateTaskRequest {
  readonly title: string;
  readonly description: string | null;
  readonly priority: TaskPriority | null;
  readonly dueDate: string | null;
}

/** Same table as TaskStatus.canTransitionTo, used only to decide which buttons to offer. */
const TRANSITIONS: Readonly<Record<TaskStatus, readonly TaskStatus[]>> = {
  TODO: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['TODO', 'DONE', 'CANCELLED'],
  DONE: [],
  CANCELLED: [],
};

export function allowedTransitions(from: TaskStatus): readonly TaskStatus[] {
  return TRANSITIONS[from];
}

export function label(value: TaskStatus | TaskPriority): string {
  return value.charAt(0) + value.slice(1).toLowerCase().replace('_', ' ');
}
