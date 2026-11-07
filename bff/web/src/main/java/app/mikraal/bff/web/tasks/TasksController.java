package app.mikraal.bff.web.tasks;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import app.mikraal.bff.web.activities.api.CapabilitiesClient;
import app.mikraal.bff.web.activities.api.TasksClient;
import app.mikraal.bff.web.activities.model.ActivitiesTask;
import app.mikraal.bff.web.activities.model.ActivitiesTaskCapabilities;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryPage;
import app.mikraal.bff.web.activities.model.ActivitiesTaskPage;
import app.mikraal.bff.web.api.TasksApi;
import app.mikraal.bff.web.api.model.ChangeTaskStatus;
import app.mikraal.bff.web.api.model.CreateTask;
import app.mikraal.bff.web.api.model.Task;
import app.mikraal.bff.web.api.model.TaskCapabilities;
import app.mikraal.bff.web.api.model.TaskHistoryPage;
import app.mikraal.bff.web.api.model.TaskPage;
import app.mikraal.bff.web.api.model.TaskStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Task endpoints: proxied to the activities service, names resolved on the way back. */
@RestController
class TasksController implements TasksApi {

    private final TasksClient tasks;
    private final CapabilitiesClient capabilities;
    private final NamesFactory namesFactory;

    TasksController(TasksClient tasks, CapabilitiesClient capabilities, NamesFactory namesFactory) {
        this.tasks = tasks;
        this.capabilities = capabilities;
        this.namesFactory = namesFactory;
    }

    @Override
    public ResponseEntity<TaskCapabilities> getTaskCapabilities(UUID groupId) {
        ResponseEntity<ActivitiesTaskCapabilities> response = capabilities.getTaskCapabilities(groupId);
        return Relay.relay(response, TaskMapper.toCapabilities(response.getBody()));
    }

    @Override
    public ResponseEntity<TaskPage> listTasks(UUID groupId, List<TaskStatus> status, String assignedTo, UUID tagId,
            String linked, LocalDate dueFrom, LocalDate dueTo, Boolean overdue, String cursor, Integer limit) {
        ResponseEntity<ActivitiesTaskPage> response = tasks.listTasks(groupId, TaskRequests.statuses(status),
                assignedTo, tagId, linked, dueFrom, dueTo, overdue, cursor, limit);
        ActivitiesTaskPage page = response.getBody();
        Names names = namesFactory.forTasks(groupId, page.getItems());
        return Relay.relay(response, new TaskPage()
                .items(Lists.map(page.getItems(), task -> TaskMapper.toTask(task, names)))
                .nextCursor(page.getNextCursor()));
    }

    @Override
    public ResponseEntity<Task> createTask(String xRequestedWith, UUID groupId, CreateTask createTask) {
        return enriched(groupId, tasks.createTask(groupId, TaskRequests.create(createTask)));
    }

    @Override
    public ResponseEntity<Task> getTask(UUID groupId, UUID taskId) {
        return enriched(groupId, tasks.getTask(groupId, taskId));
    }

    @Override
    public ResponseEntity<Task> updateTask(String ifMatch, String xRequestedWith, UUID groupId, UUID taskId,
            JsonNode mergePatch) {
        return enriched(groupId, tasks.updateTask(ifMatch, groupId, taskId, mergePatch));
    }

    @Override
    public ResponseEntity<Task> changeTaskStatus(String ifMatch, String xRequestedWith, UUID groupId, UUID taskId,
            ChangeTaskStatus changeTaskStatus) {
        return enriched(groupId,
                tasks.changeTaskStatus(ifMatch, groupId, taskId, TaskRequests.changeStatus(changeTaskStatus)));
    }

    @Override
    public ResponseEntity<Void> deleteTask(String ifMatch, String xRequestedWith, UUID groupId, UUID taskId) {
        return Relay.relay(tasks.deleteTask(ifMatch, groupId, taskId));
    }

    @Override
    public ResponseEntity<Void> endTaskRecurrence(String xRequestedWith, UUID groupId, UUID taskId) {
        return Relay.relay(tasks.endTaskRecurrence(groupId, taskId));
    }

    @Override
    public ResponseEntity<TaskHistoryPage> getTaskHistory(UUID groupId, UUID taskId, String cursor, Integer limit) {
        ResponseEntity<ActivitiesTaskHistoryPage> response = tasks.getTaskHistory(groupId, taskId, cursor, limit);
        ActivitiesTaskHistoryPage page = response.getBody();
        Names names = namesFactory.forHistory(groupId, page.getItems());
        return Relay.relay(response, new TaskHistoryPage()
                .items(Lists.map(page.getItems(), entry -> HistoryMapper.toEntry(entry, names)))
                .nextCursor(page.getNextCursor()));
    }

    private ResponseEntity<Task> enriched(UUID groupId, ResponseEntity<ActivitiesTask> response) {
        ActivitiesTask task = response.getBody();
        return Relay.relay(response, TaskMapper.toTask(task, namesFactory.forTasks(groupId, List.of(task))));
    }
}
