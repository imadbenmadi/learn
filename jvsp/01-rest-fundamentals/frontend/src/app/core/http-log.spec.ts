import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { HttpLog, httpLogInterceptor } from './http-log';

describe('httpLogInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let log: HttpLog;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpLogInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    log = TestBed.inject(HttpLog);
  });

  afterEach(() => backend.verify());

  it('records a successful exchange with its Location header', async () => {
    const response = firstValueFrom(http.post('/api/tasks', { title: 'x' }));
    backend
      .expectOne('/api/tasks')
      .flush(
        { id: '1' },
        { status: 201, statusText: 'Created', headers: { Location: '/api/tasks/1' } },
      );
    await response;

    expect(log.exchanges()).toEqual([
      expect.objectContaining({
        method: 'POST',
        url: '/api/tasks',
        status: 201,
        location: '/api/tasks/1',
        requestBody: { title: 'x' },
        responseBody: { id: '1' },
      }),
    ]);
  });

  it('records failed exchanges too, newest first', async () => {
    const first = firstValueFrom(http.get('/api/tasks'));
    backend.expectOne('/api/tasks').flush([]);
    await first;

    const second = firstValueFrom(http.get('/api/tasks/nope'));
    backend
      .expectOne('/api/tasks/nope')
      .flush({ status: 400 }, { status: 400, statusText: 'Bad Request' });
    await expect(second).rejects.toBeDefined();

    expect(log.exchanges().map((x) => x.status)).toEqual([400, 200]);
  });
});
