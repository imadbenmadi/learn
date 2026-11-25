package dev.jvsp.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource(textBlock = """
            TODO,        IN_PROGRESS, true
            TODO,        CANCELLED,   true
            TODO,        DONE,        false
            IN_PROGRESS, TODO,        true
            IN_PROGRESS, DONE,        true
            IN_PROGRESS, CANCELLED,   true
            DONE,        TODO,        false
            DONE,        IN_PROGRESS, false
            CANCELLED,   TODO,        false
            """)
    void transitions(TaskStatus from, TaskStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
