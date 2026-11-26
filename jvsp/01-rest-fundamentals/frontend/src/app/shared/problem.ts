import { HttpErrorResponse } from '@angular/common/http';

/** One entry of the `errors` member that GlobalExceptionHandler adds for validation failures. */
export interface FieldViolation {
  readonly field: string;
  readonly message: string;
}

/**
 * An RFC 9457 problem as Spring's `ProblemDetail` serialises it. Members added with
 * `ProblemDetail.setProperty` (taskId, limit, currentStatus, ...) are kept in `extensions`.
 */
export interface Problem {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly detail: string | null;
  readonly errors: readonly FieldViolation[];
  readonly extensions: Readonly<Record<string, unknown>>;
  /** True when the body really was `application/problem+json`, false when built on the client. */
  readonly fromServer: boolean;
}

const STANDARD_MEMBERS = new Set(['type', 'title', 'status', 'detail', 'instance', 'errors']);

export function toProblem(error: unknown): Problem {
  if (!(error instanceof HttpErrorResponse)) {
    return clientProblem(0, 'Unexpected error', String(error));
  }
  if (error.status === 0) {
    return clientProblem(
      0,
      'Backend unreachable',
      'No response. Is the Spring Boot app running on :8080 (./mvnw spring-boot:run)?',
    );
  }
  const body: unknown = error.error;
  const contentType = error.headers.get('Content-Type') ?? '';
  if (contentType.includes('application/problem+json') && isRecord(body)) {
    return {
      type: typeof body['type'] === 'string' ? body['type'] : 'about:blank',
      title: typeof body['title'] === 'string' ? body['title'] : error.statusText,
      status: typeof body['status'] === 'number' ? body['status'] : error.status,
      detail: typeof body['detail'] === 'string' ? body['detail'] : null,
      errors: Array.isArray(body['errors']) ? body['errors'].filter(isFieldViolation) : [],
      extensions: Object.fromEntries(
        Object.entries(body).filter(([key]) => !STANDARD_MEMBERS.has(key)),
      ),
      fromServer: true,
    };
  }
  return clientProblem(error.status, error.statusText || 'HTTP error', error.message);
}

function clientProblem(status: number, title: string, detail: string): Problem {
  return {
    type: 'about:blank',
    title,
    status,
    detail,
    errors: [],
    extensions: {},
    fromServer: false,
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isFieldViolation(value: unknown): value is FieldViolation {
  return (
    isRecord(value) && typeof value['field'] === 'string' && typeof value['message'] === 'string'
  );
}
