package dev.jvsp.taskapi.task;

import java.util.UUID;

public final class InvalidStatusTransitionException extends TaskException {

    private final UUID taskId;
    private final TaskStatus from;
    private final TaskStatus to;

    InvalidStatusTransitionException(UUID taskId, TaskStatus from, TaskStatus to) {
        super("Task %s cannot move from %s to %s.".formatted(taskId, from, to));
        this.taskId = taskId;
        this.from = from;
        this.to = to;
    }

    public UUID taskId() {
        return taskId;
    }

    public TaskStatus from() {
        return from;
    }

    public TaskStatus to() {
        return to;
    }
}
