package dev.jvsp.taskapi.task;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Constructor injection: Spring sees a single constructor and resolves each parameter by type from
// the context (no @Autowired needed). The fields can be final, the class can be built with "new" in
// a unit test, and a missing dependency fails at startup rather than with a null at runtime.
@Service
class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository repository;
    private final TaskProperties properties;
    private final Clock clock;

    TaskService(TaskRepository repository, TaskProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    // synchronized: count-then-insert must be atomic, or two concurrent requests could both pass the
    // limit check. This bean is a singleton, so the lock is shared by all request threads. With a
    // database (project 02), a transaction and a constraint or lock do this job instead.
    synchronized TaskResponse create(CreateTaskRequest request) {
        if (repository.countOpen() >= properties.maxOpenTasks()) {
            throw new OpenTaskLimitExceededException(properties.maxOpenTasks());
        }
        Instant now = clock.instant();
        var task = new Task(
                UUID.randomUUID(),
                request.title().strip(),
                request.description(),
                Objects.requireNonNullElse(request.priority(), TaskPriority.MEDIUM),
                TaskStatus.TODO,
                Objects.requireNonNullElseGet(request.dueDate(), () -> defaultDueDate(now)),
                now,
                now);
        repository.save(task);
        log.info("Created task {} with priority {}", task.id(), task.priority());
        return TaskResponse.from(task);
    }

    List<TaskResponse> findAll(TaskStatus status) {
        List<Task> tasks = status == null ? repository.findAll() : repository.findByStatus(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    TaskResponse findById(UUID id) {
        return TaskResponse.from(load(id));
    }

    TaskResponse update(UUID id, UpdateTaskRequest request) {
        Task updated = load(id).withDetails(request.title().strip(), request.description(), request.priority(),
                request.dueDate(), clock.instant());
        repository.save(updated);
        log.debug("Updated task {}", id);
        return TaskResponse.from(updated);
    }

    TaskResponse changeStatus(UUID id, TaskStatus target) {
        Task task = load(id);
        if (!task.status().canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(id, task.status(), target);
        }
        Task updated = repository.save(task.withStatus(target, clock.instant()));
        log.info("Task {} moved from {} to {}", id, task.status(), target);
        return TaskResponse.from(updated);
    }

    void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new TaskNotFoundException(id);
        }
        log.info("Deleted task {}", id);
    }

    private Task load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private LocalDate defaultDueDate(Instant now) {
        return LocalDate.ofInstant(now.plus(properties.defaultDueIn()), clock.getZone());
    }
}
