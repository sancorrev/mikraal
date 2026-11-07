package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.ANA;
import static app.mikraal.activities.tasks.Seed.DANIEL;
import static app.mikraal.activities.tasks.Seed.GROUP;
import static app.mikraal.activities.tasks.Seed.IRENE;
import static app.mikraal.activities.tasks.Seed.JAVIER;
import static app.mikraal.activities.tasks.Seed.LUCIA;
import static app.mikraal.activities.tasks.Seed.MARTA;
import static app.mikraal.activities.tasks.Seed.MATERIAL;
import static app.mikraal.activities.tasks.Seed.PABLO;
import static app.mikraal.activities.tasks.Seed.SECRETARIA;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

class TaskPermissionsTest extends IntegrationTest {

    @Test
    void familyMembersAreDeniedEverywhere() throws Exception {
        UUID taskId = id(createTask(JAVIER, Map.of("title", "Preparar el campamento")));
        String group = "/groups/" + GROUP;

        for (String path : List.of(tasks(), task(taskId), task(taskId) + "/history", group + "/task-tags",
                group + "/task-capabilities")) {
            MvcResult result = get(ANA, path);
            assertThat(result.getResponse().getStatus()).as(path).isEqualTo(403);
            assertThat(result.getResponse().getContentType()).isEqualTo("application/problem+json");
        }
        assertThat(post(ANA, tasks(), Map.of("title", "x")).getResponse().getStatus()).isEqualTo(403);
        assertThat(changeStatus(ANA, taskId, "\"1\"", Map.of("status", "finished")).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void everyScouterSeesEveryTaskButOnlyActsWithPermission() throws Exception {
        UUID taskId = id(createTask(JAVIER, Map.of("title", "Renovar el seguro")));

        JsonNode seenByIrene = body(get(IRENE, task(taskId)));

        assertThat(seenByIrene.get("title").stringValue()).isEqualTo("Renovar el seguro");
        assertThat(actions(seenByIrene)).isEmpty();
        assertThat(patch(IRENE, task(taskId), "\"1\"", Map.of("title", "x")).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(changeStatus(IRENE, taskId, "\"1\"", Map.of("status", "finished")).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(delete(IRENE, task(taskId), "\"1\"").getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void creatorMayEditDeleteChangeStatusAndEndRecurrence() throws Exception {
        JsonNode created = createTask(JAVIER, Map.of("title", "Revisar tiendas", "dueDate", "2030-01-10",
                "recurrence", Map.of("period", "monthly")));

        assertThat(actions(created)).containsExactlyInAnyOrder("change_status", "edit", "delete", "end_recurrence");
    }

    @Test
    void groupCoordinationMayEditAndDeleteButNotChangeStatus() throws Exception {
        JsonNode created = createTask(JAVIER, Map.of("title", "Pedir presupuesto de autobús"));
        UUID taskId = id(created);

        assertThat(actions(body(get(LUCIA, task(taskId))))).containsExactlyInAnyOrder("edit", "delete");
        assertThat(changeStatus(LUCIA, taskId, etag(created), Map.of("status", "finished")).getResponse()
                .getStatus()).isEqualTo(403);
        MvcResult edited = patch(LUCIA, task(taskId), etag(created), Map.of("title", "Pedir dos presupuestos"));
        assertThat(edited.getResponse().getStatus()).isEqualTo(200);
        assertThat(delete(LUCIA, task(taskId), etag(body(edited))).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void holdersOfAnOwnerPositionMayChangeStatus() throws Exception {
        JsonNode created = createTask(JAVIER, Map.of("title", "Enviar circular",
                "owners", List.of(owner("position", SECRETARIA))));
        UUID taskId = id(created);

        assertThat(actions(body(get(MARTA, task(taskId))))).containsExactly("change_status");
        assertThat(changeStatus(PABLO, taskId, etag(created), Map.of("status", "in_progress")).getResponse()
                .getStatus()).isEqualTo(403);
        assertThat(changeStatus(MARTA, taskId, etag(created), Map.of("status", "in_progress")).getResponse()
                .getStatus()).isEqualTo(200);
        assertThat(patch(MARTA, task(taskId), "\"2\"", Map.of("title", "x")).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void assignedToFiltersIncludeTasksOfThePositionsHeld() throws Exception {
        Map<String, String> event = Map.of("type", "event", "id", UUID.randomUUID().toString());
        UUID pablos = id(createTask(JAVIER, Map.of("title", "A", "links", List.of(event),
                "owners", List.of(owner("person", PABLO)))));
        UUID materials = id(createTask(JAVIER, Map.of("title", "B", "links", List.of(event),
                "owners", List.of(owner("position", MATERIAL)))));
        id(createTask(JAVIER, Map.of("title", "C", "links", List.of(event),
                "owners", List.of(owner("person", IRENE)))));
        UUID nobodys = id(createTask(JAVIER, Map.of("title", "D", "links", List.of(event))));
        String linked = "&linked=event:" + event.get("id");

        assertThat(listIds(DANIEL, "assignedTo=me" + linked)).containsExactly(materials);
        assertThat(listIds(PABLO, "assignedTo=me" + linked)).containsExactlyInAnyOrder(pablos, materials);
        assertThat(listIds(IRENE, "assignedTo=person:" + PABLO + linked)).containsExactlyInAnyOrder(pablos, materials);
        assertThat(listIds(IRENE, "assignedTo=position:" + MATERIAL + linked)).containsExactly(materials);
        assertThat(listIds(IRENE, "assignedTo=unassigned" + linked)).containsExactly(nobodys);
    }

    @Test
    void capabilitiesFollowThePermissions() throws Exception {
        String path = "/groups/" + GROUP + "/task-capabilities";

        assertThat(body(get(JAVIER, path)).toString()).isEqualTo("{\"canCreateTasks\":true,\"canManageTags\":false}");
        assertThat(body(get(LUCIA, path)).toString()).isEqualTo("{\"canCreateTasks\":true,\"canManageTags\":true}");
    }

    @Test
    void newOwnersAreNotifiedIncludingEveryHolderOfAPosition() throws Exception {
        JsonNode created = createTask(JAVIER, Map.of("title", "Inventario de material",
                "owners", List.of(owner("position", MATERIAL))));
        UUID taskId = id(created);
        patch(JAVIER, task(taskId), etag(created),
                Map.of("owners", List.of(owner("position", MATERIAL), owner("person", IRENE))));

        assertThat(notifications.about(taskId)).extracting(RecordingNotifications.Sent::recipients)
                .containsExactly(Set.of(PABLO, DANIEL), Set.of(IRENE));
    }

    @Test
    void requestsNeedAValidTokenForThisService() throws Exception {
        assertThat(mvc.perform(MockMvcRequestBuilders.get(tasks())).andReturn().getResponse().getStatus()).isEqualTo(401);

        var claims = new JWTClaimsSet.Builder().issuer("mikraal-dev").audience("another-service")
                .subject(JAVIER.toString()).expirationTime(Date.from(Instant.now().plusSeconds(60))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(tasks()).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.serialize()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/problem+json");
    }

    private List<UUID> listIds(UUID person, String query) throws Exception {
        JsonNode page = body(get(person, tasks() + "?" + query));
        return StreamSupport.stream(page.get("items").spliterator(), false).map(IntegrationTest::id).toList();
    }

    private static Set<String> actions(JsonNode task) {
        return StreamSupport.stream(task.get("allowedActions").spliterator(), false)
                .map(JsonNode::stringValue).collect(Collectors.toSet());
    }

    static Map<String, String> owner(String type, UUID id) {
        return Map.of("type", type, "id", id.toString());
    }
}
