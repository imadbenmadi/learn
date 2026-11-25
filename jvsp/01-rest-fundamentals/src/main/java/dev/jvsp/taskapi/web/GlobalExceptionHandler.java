package dev.jvsp.taskapi.web;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import dev.jvsp.taskapi.task.InvalidStatusTransitionException;
import dev.jvsp.taskapi.task.OpenTaskLimitExceededException;
import dev.jvsp.taskapi.task.TaskException;
import dev.jvsp.taskapi.task.TaskNotFoundException;

// When a controller throws, DispatcherServlet asks its HandlerExceptionResolvers for a response.
// ExceptionHandlerExceptionResolver finds the closest matching @ExceptionHandler, first in the
// controller itself, then in @ControllerAdvice beans, which is what makes this class "global".
//
// Extending ResponseEntityExceptionHandler adds handlers for every exception Spring MVC itself
// throws (malformed JSON, wrong media type, type mismatch, missing parameter, unknown path, ...)
// and renders all of them as RFC 9457 ProblemDetail ("application/problem+json"). Because this bean
// exists, Boot's own equivalent (enabled by spring.mvc.problemdetails.enabled) is not needed.
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_TYPE_PREFIX = "urn:jvsp:problem:";

    @ExceptionHandler
    ProblemDetail handleTaskException(TaskException exception) {
        // Exhaustive switch over the sealed hierarchy: no default branch needed.
        return switch (exception) {
            case TaskNotFoundException e -> {
                ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "task-not-found", "Task not found", e);
                problem.setProperty("taskId", e.taskId());
                yield problem;
            }
            case InvalidStatusTransitionException e -> {
                ProblemDetail problem = problem(HttpStatus.CONFLICT, "invalid-status-transition",
                        "Invalid status transition", e);
                problem.setProperty("taskId", e.taskId());
                problem.setProperty("currentStatus", e.from());
                problem.setProperty("requestedStatus", e.to());
                yield problem;
            }
            case OpenTaskLimitExceededException e -> {
                ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "open-task-limit-exceeded",
                        "Open task limit exceeded", e);
                problem.setProperty("limit", e.limit());
                yield problem;
            }
        };
    }

    // Thrown when @Valid fails on a @RequestBody. The inherited handler already produces a 400
    // ProblemDetail; this override adds the individual field errors to it.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = exception.getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        ProblemDetail problem = exception.getBody();
        problem.setType(URI.create(PROBLEM_TYPE_PREFIX + "validation-failed"));
        problem.setTitle("Validation failed");
        problem.setProperty("errors", violations);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    // Last resort. Without it the exception would reach the servlet container and Boot's
    // BasicErrorController (/error) would answer in a different JSON shape. The stack trace goes to
    // the log, never to the client.
    @ExceptionHandler
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(URI.create(PROBLEM_TYPE_PREFIX + type));
        problem.setTitle(title);
        return problem;
    }

    record FieldViolation(String field, String message) {
    }
}
