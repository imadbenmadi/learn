package dev.jvsp.taskapi.task;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/tasks}. {@code priority} and {@code dueDate} are optional and get
 * defaults in {@link TaskService}.
 */
public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskPriority priority,
        @FutureOrPresent LocalDate dueDate) {
}
