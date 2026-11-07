package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.GROUP;
import static app.mikraal.activities.tasks.Seed.IRENE;
import static app.mikraal.activities.tasks.Seed.JAVIER;
import static app.mikraal.activities.tasks.Seed.LUCIA;
import static app.mikraal.activities.tasks.Seed.PABLO;
import static app.mikraal.activities.tasks.Seed.SECRETARIA;
import static app.mikraal.activities.tasks.TaskPermissionsTest.owner;
import static org.assertj.core.api.Assertions.assertThat;

import app.mikraal.activities.tasks.application.OwnerLeave;
import app.mikraal.activities.tasks.domain.Actor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class OwnerLeaveTest extends IntegrationTest {

    @Autowired
    OwnerLeave ownerLeave;

    @Test
    void aLeavingPersonIsRemovedFromTheirOpenTasksOnly() throws Exception {
        UUID leaving = IRENE;
        UUID alone = id(createTask(JAVIER, Map.of("title", "a", "owners", List.of(owner("person", leaving)))));
        UUID shared = id(createTask(JAVIER, Map.of("title", "b",
                "owners", List.of(owner("person", leaving), owner("person", PABLO)))));
        UUID byPosition = id(createTask(JAVIER, Map.of("title", "c",
                "owners", List.of(owner("position", SECRETARIA)))));
        JsonNode done = createTask(JAVIER, Map.of("title", "d", "owners", List.of(owner("person", leaving))));
        changeStatus(JAVIER, id(done), etag(done), Map.of("status", "finished"));

        List<UUID> affected = ownerLeave.removeFromOpenTasks(new Actor(LUCIA), GROUP, leaving);

        assertThat(affected).contains(alone, shared).doesNotContain(byPosition, id(done));
        JsonNode aloneNow = body(get(JAVIER, task(alone)));
        assertThat(aloneNow.get("owners").size()).isZero();
        assertThat(aloneNow.get("unassigned").booleanValue()).isTrue();
        assertThat(body(get(JAVIER, task(shared))).get("owners").toString()).contains(PABLO.toString())
                .doesNotContain(leaving.toString());
        assertThat(body(get(JAVIER, task(id(done)))).get("owners").toString()).contains(leaving.toString());

        JsonNode entry = body(get(JAVIER, task(alone) + "/history")).get("items").get(0);
        assertThat(entry.get("kind").stringValue()).isEqualTo("owner_removed_on_leave");
        assertThat(entry.get("actor").get("personId").stringValue()).isEqualTo(LUCIA.toString());
        assertThat(entry.get("changes").get("owners").get("to").size()).isZero();
        assertThat(changeStatus(leaving, alone, etag(aloneNow), Map.of("status", "finished")).getResponse()
                .getStatus()).isEqualTo(403);
    }
}
