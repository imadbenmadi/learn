package dev.jvsp.taskapi.task;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings bound from the {@code tasks.*} keys of every property source (application.yaml, the
 * active profile's file, environment variables such as {@code TASKS_MAX_OPEN_TASKS}, command-line
 * arguments), in that order of increasing precedence.
 *
 * @param maxOpenTasks how many tasks may be TODO or IN_PROGRESS at the same time
 * @param defaultDueIn due date offset applied when a task is created without one
 */
// A record is bound through its constructor, so the object is immutable once created.
// @Validated runs Bean Validation during binding: a missing or invalid value fails application
// startup with a clear message instead of failing on the first request.
@ConfigurationProperties("tasks")
@Validated
public record TaskProperties(
        @Positive int maxOpenTasks,
        @NotNull @DurationMin(days = 1) Duration defaultDueIn) {
}
