package app.mikraal.bff.web.tasks;

import java.util.UUID;

import app.mikraal.bff.web.activities.api.TagsClient;
import app.mikraal.bff.web.activities.model.ActivitiesListTaskTags200Response;
import app.mikraal.bff.web.activities.model.ActivitiesTaskTag;
import app.mikraal.bff.web.api.TagsApi;
import app.mikraal.bff.web.api.model.ListTaskTags200Response;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.TagName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** The tag catalogue, proxied to the activities service. */
@RestController
class TagsController implements TagsApi {

    private final TagsClient tags;

    TagsController(TagsClient tags) {
        this.tags = tags;
    }

    @Override
    public ResponseEntity<ListTaskTags200Response> listTaskTags(UUID groupId) {
        ResponseEntity<ActivitiesListTaskTags200Response> response = tags.listTaskTags(groupId);
        return Relay.relay(response,
                new ListTaskTags200Response().items(Lists.map(response.getBody().getItems(), TagsController::named)));
    }

    @Override
    public ResponseEntity<NamedRef> createTaskTag(String xRequestedWith, UUID groupId, TagName tagName) {
        return named(tags.createTaskTag(groupId, TaskRequests.tag(tagName)));
    }

    @Override
    public ResponseEntity<NamedRef> renameTaskTag(String xRequestedWith, UUID groupId, UUID tagId, TagName tagName) {
        return named(tags.renameTaskTag(groupId, tagId, TaskRequests.tag(tagName)));
    }

    @Override
    public ResponseEntity<Void> deleteTaskTag(String xRequestedWith, UUID groupId, UUID tagId) {
        return Relay.relay(tags.deleteTaskTag(groupId, tagId));
    }

    private static ResponseEntity<NamedRef> named(ResponseEntity<ActivitiesTaskTag> response) {
        return Relay.relay(response, named(response.getBody()));
    }

    private static NamedRef named(ActivitiesTaskTag tag) {
        return new NamedRef(tag.getId(), tag.getName());
    }
}
