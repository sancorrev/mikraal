package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.api.generated.CapabilitiesApi;
import app.mikraal.activities.tasks.api.generated.model.TaskCapabilities;
import app.mikraal.activities.tasks.application.TaskQueries;
import app.mikraal.activities.tasks.authz.GroupAction;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CapabilitiesController implements CapabilitiesApi {

    private final TaskQueries queries;
    private final CurrentActor actor;

    CapabilitiesController(TaskQueries queries, CurrentActor actor) {
        this.queries = queries;
        this.actor = actor;
    }

    @Override
    public ResponseEntity<TaskCapabilities> getTaskCapabilities(UUID groupId) {
        var allowed = queries.capabilities(actor.get(), groupId);
        return ResponseEntity.ok(new TaskCapabilities(
                allowed.contains(GroupAction.CREATE_TASKS), allowed.contains(GroupAction.MANAGE_TAGS)));
    }
}
