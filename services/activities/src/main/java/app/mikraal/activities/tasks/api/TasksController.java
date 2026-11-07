package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.api.generated.TasksApi;
import app.mikraal.activities.tasks.api.generated.model.ChangeTaskStatus;
import app.mikraal.activities.tasks.api.generated.model.CreateTask;
import app.mikraal.activities.tasks.api.generated.model.Task;
import app.mikraal.activities.tasks.api.generated.model.TaskHistoryPage;
import app.mikraal.activities.tasks.api.generated.model.TaskPage;
import app.mikraal.activities.tasks.api.generated.model.TaskStatus;
import app.mikraal.activities.tasks.application.Page;
import app.mikraal.activities.tasks.application.TaskCommands;
import app.mikraal.activities.tasks.application.TaskListQuery;
import app.mikraal.activities.tasks.application.TaskQueries;
import app.mikraal.activities.tasks.application.TaskView;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

@RestController
class TasksController implements TasksApi {

    private final TaskCommands commands;
    private final TaskQueries queries;
    private final CurrentActor actor;

    TasksController(TaskCommands commands, TaskQueries queries, CurrentActor actor) {
        this.commands = commands;
        this.queries = queries;
        this.actor = actor;
    }

    @Override
    public ResponseEntity<TaskPage> listTasks(UUID groupId, List<TaskStatus> status, String assignedTo, UUID tagId,
                                              String linked, LocalDate dueFrom, LocalDate dueTo, Boolean overdue,
                                              String cursor, Integer limit) {
        Set<app.mikraal.activities.tasks.domain.TaskStatus> statuses = status == null ? null
                : status.stream().map(TaskMapper::toDomain).collect(Collectors.toSet());
        var query = new TaskListQuery(statuses, ListParameters.assignee(assignedTo), tagId,
                ListParameters.linked(linked), dueFrom, dueTo, overdue, cursor, limit);
        Page<TaskView> page = queries.list(actor.get(), groupId, query);
        var body = new TaskPage(page.items().stream().map(TaskMapper::toApi).toList());
        body.setNextCursor(page.nextCursor());
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Task> createTask(UUID groupId, CreateTask createTask) {
        TaskCommands.Created created = commands.create(actor.get(), groupId, TaskMapper.toCommand(createTask));
        return withETag(created.isNew() ? HttpStatus.CREATED : HttpStatus.OK, created.view());
    }

    @Override
    public ResponseEntity<Task> getTask(UUID groupId, UUID taskId) {
        return withETag(HttpStatus.OK, queries.get(actor.get(), groupId, taskId));
    }

    @Override
    public ResponseEntity<Task> updateTask(String ifMatch, UUID groupId, UUID taskId, ObjectNode updateTask) {
        TaskView view = commands.update(actor.get(), groupId, taskId, ETags.version(ifMatch),
                TaskPatchReader.read(updateTask));
        return withETag(HttpStatus.OK, view);
    }

    @Override
    public ResponseEntity<Void> deleteTask(String ifMatch, UUID groupId, UUID taskId) {
        commands.delete(actor.get(), groupId, taskId, ETags.version(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Task> changeTaskStatus(String ifMatch, UUID groupId, UUID taskId, ChangeTaskStatus body) {
        TaskView view = commands.changeStatus(actor.get(), groupId, taskId, ETags.version(ifMatch),
                TaskMapper.toDomain(body.getStatus()), body.getPauseReason());
        return withETag(HttpStatus.OK, view);
    }

    @Override
    public ResponseEntity<Void> endTaskRecurrence(UUID groupId, UUID taskId) {
        commands.endRecurrence(actor.get(), groupId, taskId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TaskHistoryPage> getTaskHistory(UUID groupId, UUID taskId, String cursor, Integer limit) {
        var page = queries.history(actor.get(), groupId, taskId, cursor, limit);
        var body = new TaskHistoryPage(page.items().stream().map(TaskMapper::toApi).toList());
        body.setNextCursor(page.nextCursor());
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Task> withETag(HttpStatus status, TaskView view) {
        return ResponseEntity.status(status).eTag(ETags.of(view.task().version())).body(TaskMapper.toApi(view));
    }
}
