package dev.jvsp.taskapi.task;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/tasks/{id}/status}. */
public record ChangeStatusRequest(@NotNull TaskStatus status) {
}
