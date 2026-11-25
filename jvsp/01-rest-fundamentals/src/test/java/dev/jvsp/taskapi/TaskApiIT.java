package dev.jvsp.taskapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import dev.jvsp.taskapi.task.TaskResponse;
import dev.jvsp.taskapi.task.TaskStatus;

// The whole application on a real embedded Tomcat at a random free port: real HTTP, real Jackson,
// every bean, the same configuration as in production (no profile active). This is the slowest kind
// of test, so it covers one complete flow rather than every edge case (those live in the slice and
// unit tests). Named *IT so failsafe, not surefire, runs it.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class TaskApiIT {

    @Autowired
    private RestTestClient client;

    @Test
    void taskLifecycleOverHttp() {
        TaskResponse created = client.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Learn the application context"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TaskResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        UUID id = created.id();

        client.patch().uri("/api/tasks/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "IN_PROGRESS"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("IN_PROGRESS");

        client.get().uri("/api/tasks?status={status}", TaskStatus.IN_PROGRESS)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[?(@.id == '%s')]".formatted(id)).exists();

        client.delete().uri("/api/tasks/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/tasks/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.type").isEqualTo("urn:jvsp:problem:task-not-found");
    }

    @Test
    void unknownPathIsAProblemDetailToo() {
        client.get().uri("/api/nothing-here")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void healthEndpointIsUp() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
