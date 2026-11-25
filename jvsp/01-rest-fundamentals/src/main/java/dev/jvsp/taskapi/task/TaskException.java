package dev.jvsp.taskapi.task;

/**
 * Every business rule violation of the task feature. The hierarchy is sealed, so the exception
 * handler can switch over it exhaustively: adding a subtype without mapping it to an HTTP response
 * is a compile error.
 */
public sealed abstract class TaskException extends RuntimeException
        permits TaskNotFoundException, InvalidStatusTransitionException, OpenTaskLimitExceededException {

    TaskException(String message) {
        super(message);
    }
}
