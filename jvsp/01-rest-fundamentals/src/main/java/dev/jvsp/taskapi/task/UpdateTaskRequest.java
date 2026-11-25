package dev.jvsp.taskapi.task;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/tasks/{id}}. PUT replaces the editable fields, so all of them except the
 * description are required. The status has its own endpoint because it follows transition rules.
 */
public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull TaskPriority priority,
        @NotNull LocalDate dueDate) {
}
