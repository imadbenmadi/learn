package dev.jvsp.taskapi.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API returns for a task. A separate type from {@link Task}, so the JSON contract does not
 * change by accident when the internal model does.
 */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    static TaskResponse from(Task task) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.priority(), task.status(),
                task.dueDate(), task.createdAt(), task.updatedAt());
    }
}
