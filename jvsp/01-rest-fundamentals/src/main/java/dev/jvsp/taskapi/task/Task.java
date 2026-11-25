package dev.jvsp.taskapi.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record Task(
        UUID id,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    Task withDetails(String newTitle, String newDescription, TaskPriority newPriority, LocalDate newDueDate,
            Instant now) {
        return new Task(id, newTitle, newDescription, newPriority, status, newDueDate, createdAt, now);
    }

    Task withStatus(TaskStatus newStatus, Instant now) {
        return new Task(id, title, description, priority, newStatus, dueDate, createdAt, now);
    }
}
