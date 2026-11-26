import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task, TaskStatus, UpdateTaskRequest } from './task.model';

const BASE = '/api/tasks';

/** One method per endpoint of TaskController. */
@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);

  /** GET /api/tasks?status=… as a resource that refetches whenever the filter signal changes. */
  listResource(status: Signal<TaskStatus | null>) {
    return httpResource<Task[]>(
      () => {
        const current = status();
        const params: Record<string, string> = current ? { status: current } : {};
        return { url: BASE, params };
      },
      { defaultValue: [] },
    );
  }

  /** GET /api/tasks/{id}. The id is sent as typed, so a non-UUID shows Spring's 400. */
  taskResource(id: Signal<string>) {
    return httpResource<Task>(() => `${BASE}/${encodeURIComponent(id())}`);
  }

  create(request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(BASE, request);
  }

  update(id: string, request: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${BASE}/${id}`, request);
  }

  changeStatus(id: string, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${BASE}/${id}/status`, { status });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
