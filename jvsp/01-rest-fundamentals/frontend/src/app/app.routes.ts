import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tasks' },
  {
    path: 'tasks',
    title: 'Tasks',
    loadComponent: () => import('./task/task-list-page').then((m) => m.TaskListPage),
  },
  {
    path: 'tasks/:id',
    title: 'Task',
    loadComponent: () => import('./task/task-detail-page').then((m) => m.TaskDetailPage),
  },
  {
    path: 'inspect',
    title: 'Application context',
    loadComponent: () => import('./inspect/inspect-page').then((m) => m.InspectPage),
  },
  { path: '**', redirectTo: 'tasks' },
];
