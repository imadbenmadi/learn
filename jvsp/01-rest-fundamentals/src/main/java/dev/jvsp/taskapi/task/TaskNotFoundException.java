package dev.jvsp.taskapi.task;

import java.util.UUID;

public final class TaskNotFoundException extends TaskException {

    private final UUID taskId;

    TaskNotFoundException(UUID taskId) {
        super("Task %s does not exist.".formatted(taskId));
        this.taskId = taskId;
    }

    public UUID taskId() {
        return taskId;
    }
}
