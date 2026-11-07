package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.JAVIER;
import static app.mikraal.activities.tasks.Seed.MARTA;
import static app.mikraal.activities.tasks.Seed.SECRETARIA;
import static app.mikraal.activities.tasks.TaskPermissionsTest.owner;
import static org.assertj.core.api.Assertions.assertThat;

import app.mikraal.activities.tasks.domain.Ids;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

class TaskLifecycleTest extends IntegrationTest {

    @Test
    void statusFollowsTheAllowedTransitionsAndPauseNeedsAReason() throws Exception {
        JsonNode task = createTask(JAVIER, Map.of("title", "Comprar cuerdas"));
        UUID id = id(task);

        assertValidation(changeStatus(JAVIER, id, etag(task), Map.of("status", "paused")),
                "pauseReason", "pause_reason_required");
        assertValidation(changeStatus(JAVIER, id, etag(task), Map.of("status", "in_progress", "pauseReason", "x")),
                "pauseReason", "pause_reason_not_allowed");
        assertThat(changeStatus(JAVIER, id, etag(task), Map.of("status", "pending")).getResponse().getStatus())
                .isEqualTo(409);

        task = changed(id, task, Map.of("status", "paused", "pauseReason", "esperando presupuesto"));
        assertThat(task.get("pauseReason").stringValue()).isEqualTo("esperando presupuesto");
        task = changed(id, task, Map.of("status", "in_progress"));
        assertThat(task.get("pauseReason").isNull()).isTrue();
        task = changed(id, task, Map.of("status", "finished"));

        MvcResult toPending = changeStatus(JAVIER, id, etag(task), Map.of("status", "pending"));
        assertThat(toPending.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(toPending).get("type").stringValue()).endsWith("/invalid-transition");

        task = changed(id, task, Map.of("status", "in_progress"));
        assertThat(task.get("status").stringValue()).isEqualTo("in_progress");
        assertThat(task.get("version").intValue()).isEqualTo(5);
    }

    @Test
    void writesNeedTheCurrentVersionInIfMatch() throws Exception {
        JsonNode task = createTask(JAVIER, Map.of("title", "Reservar albergue"));
        UUID id = id(task);

        MvcResult missing = patch(JAVIER, task(id), null, Map.of("title", "x"));
        assertThat(missing.getResponse().getStatus()).isEqualTo(428);
        assertThat(missing.getResponse().getContentType()).isEqualTo("application/problem+json");
        assertThat(changeStatus(JAVIER, id, null, Map.of("status", "finished")).getResponse().getStatus())
                .isEqualTo(428);
        assertThat(delete(JAVIER, task(id), null).getResponse().getStatus()).isEqualTo(428);

        assertThat(patch(JAVIER, task(id), "\"7\"", Map.of("title", "x")).getResponse().getStatus()).isEqualTo(412);
        assertThat(changeStatus(JAVIER, id, "\"7\"", Map.of("status", "finished")).getResponse().getStatus())
                .isEqualTo(412);
        assertThat(delete(JAVIER, task(id), "\"7\"").getResponse().getStatus()).isEqualTo(412);

        MvcResult read = get(JAVIER, task(id));
        assertThat(read.getResponse().getHeader("ETag")).isEqualTo("\"1\"");
        MvcResult edited = patch(JAVIER, task(id), "\"1\"", Map.of("title", "Reservar albergue en Cercedilla"));
        assertThat(edited.getResponse().getHeader("ETag")).isEqualTo("\"2\"");
        assertThat(patch(JAVIER, task(id), "\"1\"", Map.of("title", "y")).getResponse().getStatus()).isEqualTo(412);
    }

    @Test
    void patchIsAJsonMergePatch() throws Exception {
        JsonNode task = createTask(JAVIER, Map.of("title", "Montar la web", "description", "Con fotos",
                "dueDate", "2030-05-01", "reminderDaysBefore", 5));
        UUID id = id(task);

        task = body(patch(JAVIER, task(id), etag(task), Map.of("title", "Montar la web del grupo")));
        assertThat(task.get("description").stringValue()).isEqualTo("Con fotos");
        assertThat(task.get("reminderDaysBefore").intValue()).isEqualTo(5);

        Map<String, Object> clear = new HashMap<>();
        clear.put("description", null);
        clear.put("dueDate", null);
        task = body(patch(JAVIER, task(id), etag(task), clear));
        assertThat(task.get("title").stringValue()).isEqualTo("Montar la web del grupo");
        assertThat(task.get("description").isNull()).isTrue();
        assertThat(task.get("dueDate").isNull()).isTrue();
        assertThat(task.get("reminderDaysBefore").isNull()).isTrue();

        task = body(patch(JAVIER, task(id), etag(task), Map.of("dueDate", "2030-06-01")));
        assertThat(task.get("reminderDaysBefore").intValue()).isEqualTo(2);

        assertValidation(patch(JAVIER, task(id), etag(task), Map.of("status", "finished")), "status", "not_allowed");
        assertValidation(patch(JAVIER, task(id), etag(task), Map.of("title", " ")), "title", "required");
        assertValidation(patch(JAVIER, task(id), etag(task), Map.of("reminderDaysBefore", 31)),
                "reminderDaysBefore", "out_of_range");
        assertValidation(patch(JAVIER, task(id), etag(task), Map.of()), "body", "required");
    }

    @Test
    void createValidatesTheRulesOfTheSpec() throws Exception {
        assertValidation(post(JAVIER, tasks(), Map.of("title", "x", "recurrence", Map.of("period", "weekly"))),
                "recurrence", "recurrence_requires_due_date");
        assertValidation(post(JAVIER, tasks(), Map.of("title", "x", "reminderDaysBefore", 3)),
                "reminderDaysBefore", "reminder_requires_due_date");
        assertValidation(post(JAVIER, tasks(), Map.of("title", "x".repeat(201))), "title", "too_long");
        assertValidation(post(JAVIER, tasks(), Map.of()), "title", "required");
        assertValidation(post(JAVIER, tasks(), Map.of("title", "x", "tagIds", List.of(UUID.randomUUID()))),
                "tagIds", "unknown_tag");
        assertValidation(post(JAVIER, tasks(), Map.of("id", UUID.randomUUID(), "title", "x")), "id", "uuid_v7_required");

        JsonNode withDeadline = createTask(JAVIER, Map.of("title", "x", "dueDate", "2030-01-01"));
        assertThat(withDeadline.get("reminderDaysBefore").intValue()).isEqualTo(2);
        assertThat(withDeadline.get("status").stringValue()).isEqualTo("pending");
        assertThat(withDeadline.get("unassigned").booleanValue()).isTrue();
    }

    @Test
    void creatingWithAClientIdIsIdempotent() throws Exception {
        UUID id = Ids.newId();
        Map<String, Object> body = Map.of("id", id, "title", "Pedir permiso al ayuntamiento");

        MvcResult first = post(JAVIER, tasks(), body);
        MvcResult again = post(JAVIER, tasks(), body);

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(first.getResponse().getHeader("ETag")).isEqualTo("\"1\"");
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(again)).isEqualTo(body(first));
        assertThat(kinds(id)).containsExactly("created");
    }

    @Test
    void deletingIsFinalAndTheIdCannotBeReused() throws Exception {
        UUID id = Ids.newId();
        JsonNode task = createTask(JAVIER, Map.of("id", id, "title", "Tarea duplicada"));

        assertThat(delete(JAVIER, task(id), etag(task)).getResponse().getStatus()).isEqualTo(204);

        assertThat(get(JAVIER, task(id)).getResponse().getStatus()).isEqualTo(404);
        assertThat(get(JAVIER, task(id) + "/history").getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(JAVIER, task(id), etag(task)).getResponse().getStatus()).isEqualTo(404);
        assertThat(post(JAVIER, tasks(), Map.of("id", id, "title", "Otra")).getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void historyRecordsWhoChangedWhatNewestFirst() throws Exception {
        JsonNode task = createTask(JAVIER, Map.of("title", "Enviar circular",
                "owners", List.of(owner("position", SECRETARIA))));
        UUID id = id(task);
        MvcResult started = changeStatus(MARTA, id, etag(task), Map.of("status", "in_progress"));
        patch(JAVIER, task(id), etag(body(started)), Map.of("title", "Enviar circular de Navidad"));

        JsonNode first = body(get(JAVIER, task(id) + "/history?limit=2"));
        JsonNode items = first.get("items");
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).get("kind").stringValue()).isEqualTo("edited");
        assertThat(items.get(0).get("changes").get("title").get("to").stringValue())
                .isEqualTo("Enviar circular de Navidad");
        assertThat(items.get(1).get("kind").stringValue()).isEqualTo("status_changed");
        assertThat(items.get(1).get("actor").get("personId").stringValue()).isEqualTo(MARTA.toString());
        assertThat(items.get(1).get("actor").get("onBehalfOfPositionId").stringValue())
                .isEqualTo(SECRETARIA.toString());
        assertThat(items.get(1).get("changes").get("status").toString())
                .isEqualTo("{\"from\":\"pending\",\"to\":\"in_progress\"}");

        JsonNode second = body(get(JAVIER, task(id) + "/history?limit=2&cursor=" + first.get("nextCursor").stringValue()));
        assertThat(second.get("items").get(0).get("kind").stringValue()).isEqualTo("created");
        assertThat(second.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void overdueMeansOpenAndPastTheDeadlineInTheGroupTimeZone() throws Exception {
        // 23:30 UTC on 9 March is already 10 March in Madrid.
        clock.set(Instant.parse("2027-03-09T23:30:00Z"));
        String event = UUID.randomUUID().toString();
        List<Map<String, String>> links = List.of(Map.of("type", "event", "id", event));
        UUID late = id(createTask(JAVIER, Map.of("title", "a", "dueDate", "2027-03-09", "links", links)));
        UUID today = id(createTask(JAVIER, Map.of("title", "b", "dueDate", "2027-03-10", "links", links)));
        JsonNode done = createTask(JAVIER, Map.of("title", "c", "dueDate", "2027-03-01", "links", links));
        changeStatus(JAVIER, id(done), etag(done), Map.of("status", "finished"));

        assertThat(body(get(JAVIER, task(late))).get("overdue").booleanValue()).isTrue();
        assertThat(body(get(JAVIER, task(today))).get("overdue").booleanValue()).isFalse();
        assertThat(body(get(JAVIER, task(id(done)))).get("overdue").booleanValue()).isFalse();
        assertThat(ids(body(get(JAVIER, tasks() + "?overdue=true&linked=event:" + event)))).containsExactly(late);
    }

    @Test
    void listIsPagedByDeadlineWithACursor() throws Exception {
        String event = UUID.randomUUID().toString();
        List<Map<String, String>> links = List.of(Map.of("type", "event", "id", event));
        UUID noDeadline = id(createTask(JAVIER, Map.of("title", "c", "links", links)));
        UUID later = id(createTask(JAVIER, Map.of("title", "b", "dueDate", "2030-02-01", "links", links)));
        UUID sooner = id(createTask(JAVIER, Map.of("title", "a", "dueDate", "2030-01-01", "links", links)));
        String query = tasks() + "?limit=2&linked=event:" + event;

        JsonNode first = body(get(MARTA, query));
        JsonNode second = body(get(MARTA, query + "&cursor=" + first.get("nextCursor").stringValue()));

        assertThat(ids(first)).containsExactly(sooner, later);
        assertThat(ids(second)).containsExactly(noDeadline);
        assertThat(second.get("nextCursor").isNull()).isTrue();
        assertThat(ids(body(get(MARTA, query + "&dueFrom=2030-01-15&dueTo=" + LocalDate.of(2030, 3, 1)))))
                .containsExactly(later);
        assertValidation(get(MARTA, tasks() + "?limit=500"), "limit", "out_of_range");
        assertValidation(get(MARTA, tasks() + "?status=done"), "status", "invalid");
    }

    private JsonNode changed(UUID id, JsonNode task, Map<String, String> change) throws Exception {
        MvcResult result = changeStatus(JAVIER, id, etag(task), change);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return body(result);
    }

    private void assertValidation(MvcResult result, String field, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(400);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/problem+json");
        JsonNode problem = body(result);
        assertThat(problem.get("errors").toString()).contains("{\"field\":\"" + field + "\",\"code\":\"" + code + "\"}");
    }

    private List<String> kinds(UUID id) throws Exception {
        return StreamSupport.stream(body(get(JAVIER, task(id) + "/history")).get("items").spliterator(), false)
                .map(e -> e.get("kind").stringValue()).toList();
    }

    private static List<UUID> ids(JsonNode page) {
        return StreamSupport.stream(page.get("items").spliterator(), false).map(IntegrationTest::id).toList();
    }
}
