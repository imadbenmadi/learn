import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskForm, TaskFormValue } from './task-form';

describe('TaskForm', () => {
  let fixture: ComponentFixture<TaskForm>;
  let action: ReturnType<typeof vi.fn<(value: TaskFormValue) => Promise<unknown>>>;

  beforeEach(async () => {
    action = vi.fn<(value: TaskFormValue) => Promise<unknown>>();
    fixture = TestBed.createComponent(TaskForm);
    fixture.componentRef.setInput('mode', 'create');
    fixture.componentRef.setInput('action', action);
    await fixture.whenStable();
  });

  const element = () => fixture.nativeElement as HTMLElement;

  async function type(selector: string, value: string): Promise<void> {
    const input = element().querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function submitForm(): Promise<void> {
    element().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('does not call the API while browser validation fails', async () => {
    await submitForm();

    expect(action).not.toHaveBeenCalled();
    expect(element().textContent).toContain('must not be blank');
  });

  it('sends the form value when it is valid', async () => {
    action.mockResolvedValue(undefined);
    await type('input[type=text]', 'Read chapter 1');

    await submitForm();

    expect(action).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Read chapter 1', priority: '', dueDate: '' }),
    );
  });

  it('shows Spring field errors on the matching input when browser validation is off', async () => {
    action.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
        error: {
          type: 'urn:jvsp:problem:validation-failed',
          title: 'Validation failed',
          status: 400,
          errors: [{ field: 'title', message: 'must not be blank' }],
        },
      }),
    );
    element().querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
    await fixture.whenStable();

    await submitForm();

    expect(action).toHaveBeenCalled();
    const fieldErrors = element().querySelector('app-field-errors')!.textContent;
    expect(fieldErrors).toContain('server');
    expect(fieldErrors).toContain('must not be blank');
    expect(element().querySelector('app-problem-alert')!.textContent).toContain(
      'Validation failed',
    );
  });
});
