import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { toProblem } from './problem';

const problemJson = new HttpHeaders({ 'Content-Type': 'application/problem+json' });

describe('toProblem', () => {
  it('reads an RFC 9457 body, its field errors and its extension members', () => {
    const error = new HttpErrorResponse({
      status: 409,
      headers: problemJson,
      error: {
        type: 'urn:jvsp:problem:invalid-status-transition',
        title: 'Invalid status transition',
        status: 409,
        detail: 'Cannot move task from DONE to TODO',
        taskId: 'abc',
        currentStatus: 'DONE',
      },
    });

    const problem = toProblem(error);

    expect(problem).toMatchObject({
      type: 'urn:jvsp:problem:invalid-status-transition',
      status: 409,
      detail: 'Cannot move task from DONE to TODO',
      errors: [],
      extensions: { taskId: 'abc', currentStatus: 'DONE' },
      fromServer: true,
    });
  });

  it('keeps only well-formed validation errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      headers: problemJson,
      error: {
        title: 'Validation failed',
        status: 400,
        errors: [{ field: 'title', message: 'must not be blank' }, { nonsense: true }],
      },
    });

    expect(toProblem(error).errors).toEqual([{ field: 'title', message: 'must not be blank' }]);
  });

  it('explains status 0 as an unreachable backend', () => {
    const problem = toProblem(new HttpErrorResponse({ status: 0 }));

    expect(problem.title).toBe('Backend unreachable');
    expect(problem.fromServer).toBe(false);
  });

  it('does not treat a non-problem body as a problem', () => {
    const error = new HttpErrorResponse({
      status: 502,
      statusText: 'Bad Gateway',
      error: '<html>',
    });

    expect(toProblem(error)).toMatchObject({
      status: 502,
      title: 'Bad Gateway',
      fromServer: false,
    });
  });
});
