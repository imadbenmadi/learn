package dev.jvsp.taskapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// @SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan.
// Component scanning starts at THIS package, which is why the class sits at the root of the
// package tree. @ConfigurationPropertiesScan does the same for @ConfigurationProperties records,
// which are not @Components and would otherwise need @EnableConfigurationProperties(...).
@SpringBootApplication
@ConfigurationPropertiesScan
public class TaskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskApiApplication.class, args);
    }
}
