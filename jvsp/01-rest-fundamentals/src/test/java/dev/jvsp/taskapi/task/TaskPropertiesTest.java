package dev.jvsp.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

// ApplicationContextRunner builds a tiny, throwaway application context per test. It is the tool
// Spring Boot's own auto-configurations are tested with, and it is the fastest way to check what
// happens at startup for a given set of properties.
class TaskPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsCamelCaseAndKebabCaseNamesAndDurationShorthand() {
        contextRunner
                .withPropertyValues("tasks.max-open-tasks=3", "tasks.defaultDueIn=2d")
                .run(context -> {
                    TaskProperties properties = context.getBean(TaskProperties.class);
                    assertThat(properties.maxOpenTasks()).isEqualTo(3);
                    assertThat(properties.defaultDueIn()).isEqualTo(Duration.ofDays(2));
                });
    }

    @Test
    void failsStartupWhenLimitIsNotPositive() {
        contextRunner
                .withPropertyValues("tasks.max-open-tasks=0", "tasks.default-due-in=7d")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("maxOpenTasks"));
    }

    @Test
    void failsStartupWhenDefaultDueInIsMissing() {
        contextRunner
                .withPropertyValues("tasks.max-open-tasks=5")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("defaultDueIn"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TaskProperties.class)
    static class PropertiesConfiguration {
    }
}
