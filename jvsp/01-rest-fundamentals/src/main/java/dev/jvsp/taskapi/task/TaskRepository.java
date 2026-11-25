package dev.jvsp.taskapi.task;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

// A singleton bean is shared by every request thread (Tomcat runs one thread per request), so its
// state must be thread-safe: hence the ConcurrentHashMap. @Repository is a @Component that also
// marks the class for persistence exception translation, which starts to matter in project 02.
@Repository
class TaskRepository {

    private final ConcurrentMap<UUID, Task> tasks = new ConcurrentHashMap<>();

    Task save(Task task) {
        tasks.put(task.id(), task);
        return task;
    }

    Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    List<Task> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(Task::createdAt))
                .toList();
    }

    List<Task> findByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.status() == status)
                .sorted(Comparator.comparing(Task::createdAt))
                .toList();
    }

    long countOpen() {
        return tasks.values().stream()
                .filter(task -> task.status().isOpen())
                .count();
    }

    boolean deleteById(UUID id) {
        return tasks.remove(id) != null;
    }
}
