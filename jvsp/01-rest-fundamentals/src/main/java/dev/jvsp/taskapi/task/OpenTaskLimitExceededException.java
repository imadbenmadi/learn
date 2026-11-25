package dev.jvsp.taskapi.task;

public final class OpenTaskLimitExceededException extends TaskException {

    private final int limit;

    OpenTaskLimitExceededException(int limit) {
        super("No more than %d tasks can be open at the same time.".formatted(limit));
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
