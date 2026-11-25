package dev.jvsp.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// @WebMvcTest starts a "slice" of the application: only the MVC infrastructure (DispatcherServlet,
// message converters, validation) and web beans (@Controller, @ControllerAdvice, filters, ...).
// @Service and @Repository beans are not scanned, so TaskService is replaced by a Mockito mock that
// @MockitoBean registers in the context. No server is started: MockMvc calls DispatcherServlet
// directly, which is why these tests run in milliseconds.
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final UUID ID = UUID.fromString("0b8f5a8e-7d0c-4a43-9a65-0d6c2a1f3e11");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createReturns201WithLocationAndBody() {
        given(taskService.create(any())).willReturn(sampleTask(TaskStatus.TODO));

        var result = mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Write chapter 1", "priority": "HIGH"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/api/tasks/" + ID);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Write chapter 1");
        assertThat(result).bodyJson().extractingPath("$.dueDate").isEqualTo("2026-12-02");
    }

    @Test
    void invalidBodyIsRejectedWithFieldErrors() {
        var result = mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": " ", "dueDate": "2001-01-01"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {
                  "type": "urn:jvsp:problem:validation-failed",
                  "title": "Validation failed",
                  "status": 400,
                  "errors": [
                    {"field": "dueDate", "message": "must be a date in the present or in the future"},
                    {"field": "title", "message": "must not be blank"}
                  ]
                }
                """);
        verifyNoInteractions(taskService);
    }

    @Test
    void malformedJsonIsAProblemToo() {
        var result = mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": ")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Failed to read request");
    }

    @Test
    void unknownStatusFilterIsRejected() {
        assertThat(mvc.get().uri("/api/tasks?status=SOMEDAY"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void malformedIdIsRejected() {
        assertThat(mvc.get().uri("/api/tasks/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownTaskIs404() {
        given(taskService.findById(ID)).willThrow(new TaskNotFoundException(ID));

        var result = mvc.get().uri("/api/tasks/{id}", ID).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {
                  "type": "urn:jvsp:problem:task-not-found",
                  "title": "Task not found",
                  "status": 404,
                  "taskId": "%s"
                }
                """.formatted(ID));
    }

    @Test
    void forbiddenTransitionIs409() {
        given(taskService.changeStatus(ID, TaskStatus.TODO))
                .willThrow(new InvalidStatusTransitionException(ID, TaskStatus.DONE, TaskStatus.TODO));

        var result = mvc.patch().uri("/api/tasks/{id}/status", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "TODO"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.currentStatus").isEqualTo("DONE");
    }

    @Test
    void openTaskLimitIs422() {
        given(taskService.create(any())).willThrow(new OpenTaskLimitExceededException(5));

        var result = mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "one too many"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.limit").isEqualTo(5);
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() {
        given(taskService.findById(ID)).willThrow(new IllegalStateException("connection string with a password"));

        var result = mvc.get().uri("/api/tasks/{id}", ID).exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("An unexpected error occurred.");
    }

    @Test
    void deleteReturns204() {
        assertThat(mvc.delete().uri("/api/tasks/{id}", ID)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteOfUnknownTaskIs404() {
        willThrow(new TaskNotFoundException(ID)).given(taskService).delete(ID);

        assertThat(mvc.delete().uri("/api/tasks/{id}", ID)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private static TaskResponse sampleTask(TaskStatus status) {
        Instant now = Instant.parse("2026-11-25T10:00:00Z");
        return new TaskResponse(ID, "Write chapter 1", null, TaskPriority.HIGH, status, LocalDate.of(2026, 12, 2),
                now, now);
    }
}
