package dev.jvsp.taskapi.task;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case TODO -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == TODO || target == DONE || target == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }

    boolean isOpen() {
        return this == TODO || this == IN_PROGRESS;
    }
}
