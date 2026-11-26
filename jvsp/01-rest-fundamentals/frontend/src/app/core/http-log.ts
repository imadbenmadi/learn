import {
  HttpErrorResponse,
  HttpHeaders,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

export interface HttpExchange {
  readonly id: number;
  readonly method: string;
  readonly url: string;
  readonly status: number;
  readonly durationMs: number;
  readonly contentType: string | null;
  readonly location: string | null;
  readonly requestBody: unknown;
  readonly responseBody: unknown;
}

const MAX_ENTRIES = 50;

/** Keeps the last HTTP exchanges so the UI can show what actually went over the wire. */
@Injectable({ providedIn: 'root' })
export class HttpLog {
  private nextId = 1;
  private readonly entries = signal<readonly HttpExchange[]>([]);
  readonly exchanges = this.entries.asReadonly();

  record(
    request: HttpRequest<unknown>,
    status: number,
    headers: HttpHeaders,
    responseBody: unknown,
    startedAt: number,
  ): void {
    const exchange: HttpExchange = {
      id: this.nextId++,
      method: request.method,
      url: request.urlWithParams,
      status,
      durationMs: Math.round(performance.now() - startedAt),
      contentType: headers.get('Content-Type'),
      location: headers.get('Location'),
      requestBody: request.body,
      responseBody,
    };
    this.entries.update((list) => [exchange, ...list].slice(0, MAX_ENTRIES));
  }

  clear(): void {
    this.entries.set([]);
  }
}

export const httpLogInterceptor: HttpInterceptorFn = (request, next) => {
  const log = inject(HttpLog);
  const startedAt = performance.now();
  return next(request).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          log.record(request, event.status, event.headers, event.body, startedAt);
        }
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          log.record(request, error.status, error.headers, error.error, startedAt);
        }
      },
    }),
  );
};
