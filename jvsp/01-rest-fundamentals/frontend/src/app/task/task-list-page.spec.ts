import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TaskListPage } from './task-list-page';
import { Task } from './task.model';

const task: Task = {
  id: '0b6d7a3e-0000-4000-8000-000000000001',
  title: 'Read chapter 1',
  description: null,
  priority: 'HIGH',
  status: 'TODO',
  dueDate: '2026-12-03',
  createdAt: '2026-11-26T10:00:00Z',
  updatedAt: '2026-11-26T10:00:00Z',
};

describe('TaskListPage', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lists tasks and refetches with ?status= when a filter is chosen', async () => {
    // whenStable() would wait for the pending httpResource request, so only run change detection.
    const fixture = TestBed.createComponent(TaskListPage);
    TestBed.tick();
    backend.expectOne('/api/tasks').flush([task]);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.task__title')!.textContent).toContain('Read chapter 1');

    const doneChip = Array.from(element.querySelectorAll<HTMLButtonElement>('.chip')).find(
      (b) => b.textContent!.trim() === 'Done',
    )!;
    doneChip.click();
    TestBed.tick();
    backend.expectOne('/api/tasks?status=DONE').flush([]);
    await fixture.whenStable();

    expect(element.textContent).toContain('No tasks with status DONE');
  });
});
