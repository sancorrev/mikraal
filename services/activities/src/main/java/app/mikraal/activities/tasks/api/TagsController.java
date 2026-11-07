package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.api.generated.TagsApi;
import app.mikraal.activities.tasks.api.generated.model.CreateTaskTag;
import app.mikraal.activities.tasks.api.generated.model.ListTaskTags200Response;
import app.mikraal.activities.tasks.api.generated.model.TaskTag;
import app.mikraal.activities.tasks.application.TagCommands;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TagsController implements TagsApi {

    private final TagCommands tags;
    private final CurrentActor actor;

    TagsController(TagCommands tags, CurrentActor actor) {
        this.tags = tags;
        this.actor = actor;
    }

    @Override
    public ResponseEntity<ListTaskTags200Response> listTaskTags(UUID groupId) {
        return ResponseEntity.ok(new ListTaskTags200Response(
                tags.list(actor.get(), groupId).stream().map(TaskMapper::toApi).toList()));
    }

    @Override
    public ResponseEntity<TaskTag> createTaskTag(UUID groupId, CreateTaskTag body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TaskMapper.toApi(tags.create(actor.get(), groupId, body.getName())));
    }

    @Override
    public ResponseEntity<TaskTag> renameTaskTag(UUID groupId, UUID tagId, CreateTaskTag body) {
        return ResponseEntity.ok(TaskMapper.toApi(tags.rename(actor.get(), groupId, tagId, body.getName())));
    }

    @Override
    public ResponseEntity<Void> deleteTaskTag(UUID groupId, UUID tagId) {
        tags.delete(actor.get(), groupId, tagId);
        return ResponseEntity.noContent().build();
    }
}
