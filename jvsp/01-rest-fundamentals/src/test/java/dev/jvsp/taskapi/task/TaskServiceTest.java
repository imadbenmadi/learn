package dev.jvsp.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// A plain unit test: no Spring context at all. Constructor injection is what makes this possible,
// the collaborators are simply passed to "new".
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    private TaskService service;

    @BeforeEach
    void setUp() {
        var properties = new TaskProperties(2, Duration.ofDays(7));
        service = new TaskService(new TaskRepository(), properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createAppliesDefaultsForMissingPriorityAndDueDate() {
        TaskResponse created = service.create(new CreateTaskRequest("  Write chapter 1  ", null, null, null));

        assertThat(created.title()).isEqualTo("Write chapter 1");
        assertThat(created.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        assertThat(created.dueDate()).isEqualTo(LocalDate.of(2026, 12, 2));
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void createKeepsGivenPriorityAndDueDate() {
        LocalDate dueDate = LocalDate.of(2027, 1, 15);

        TaskResponse created = service.create(new CreateTaskRequest("Review", "PR 42", TaskPriority.HIGH, dueDate));

        assertThat(created.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(created.dueDate()).isEqualTo(dueDate);
        assertThat(created.description()).isEqualTo("PR 42");
    }

    @Test
    void createFailsWhenOpenTaskLimitIsReached() {
        service.create(task("one"));
        service.create(task("two"));

        assertThatThrownBy(() -> service.create(task("three")))
                .isInstanceOfSatisfying(OpenTaskLimitExceededException.class,
                        e -> assertThat(e.limit()).isEqualTo(2));
    }

    @Test
    void finishedTasksDoNotCountTowardsTheLimit() {
        TaskResponse first = service.create(task("one"));
        service.create(task("two"));
        service.changeStatus(first.id(), TaskStatus.CANCELLED);

        assertThat(service.create(task("three")).status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void changeStatusFollowsTheTransitionRules() {
        UUID id = service.create(task("one")).id();

        assertThat(service.changeStatus(id, TaskStatus.IN_PROGRESS).status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.changeStatus(id, TaskStatus.DONE).status()).isEqualTo(TaskStatus.DONE);
        assertThatThrownBy(() -> service.changeStatus(id, TaskStatus.TODO))
                .isInstanceOfSatisfying(InvalidStatusTransitionException.class, e -> {
                    assertThat(e.from()).isEqualTo(TaskStatus.DONE);
                    assertThat(e.to()).isEqualTo(TaskStatus.TODO);
                });
    }

    @Test
    void updateReplacesDetailsButKeepsStatusAndCreationTime() {
        UUID id = service.create(task("one")).id();
        service.changeStatus(id, TaskStatus.IN_PROGRESS);

        TaskResponse updated = service.update(id,
                new UpdateTaskRequest("renamed", "details", TaskPriority.LOW, LocalDate.of(2027, 2, 1)));

        assertThat(updated.title()).isEqualTo("renamed");
        assertThat(updated.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.createdAt()).isEqualTo(NOW);
    }

    @Test
    void findAllFiltersByStatus() {
        UUID started = service.create(task("one")).id();
        service.create(task("two"));
        service.changeStatus(started, TaskStatus.IN_PROGRESS);

        assertThat(service.findAll(TaskStatus.IN_PROGRESS)).extracting(TaskResponse::id).containsExactly(started);
        assertThat(service.findAll(null)).hasSize(2);
    }

    @Test
    void unknownIdsAreReportedAsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(unknown)).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.delete(unknown)).isInstanceOf(TaskNotFoundException.class);
    }

    private static CreateTaskRequest task(String title) {
        return new CreateTaskRequest(title, null, null, null);
    }
}
