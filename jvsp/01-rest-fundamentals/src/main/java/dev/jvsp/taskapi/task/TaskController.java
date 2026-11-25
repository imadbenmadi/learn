package dev.jvsp.taskapi.task;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// At startup, RequestMappingHandlerMapping scans every @Controller bean and registers one handler per
// mapped method. At request time DispatcherServlet asks it which method matches, argument resolvers
// build the parameters (the JSON body through Jackson's HttpMessageConverter, then @Valid runs Bean
// Validation), and the return value is written back through the same converter.
// @RestController = @Controller + @ResponseBody on every method: return values are the response body,
// not view names.
@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    List<TaskResponse> list(@RequestParam(required = false) TaskStatus status) {
        return taskService.findAll(status);
    }

    @GetMapping("/{id}")
    TaskResponse get(@PathVariable UUID id) {
        return taskService.findById(id);
    }

    @PutMapping("/{id}")
    TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    TaskResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return taskService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        taskService.delete(id);
    }
}
