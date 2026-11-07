package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.GROUP;
import static app.mikraal.activities.tasks.Seed.JAVIER;
import static app.mikraal.activities.tasks.Seed.MARTA;
import static app.mikraal.activities.tasks.Seed.PABLO;
import static app.mikraal.activities.tasks.Seed.SECRETARIA;
import static app.mikraal.activities.tasks.TaskPermissionsTest.owner;
import static org.assertj.core.api.Assertions.assertThat;

import app.mikraal.activities.tasks.application.RecurrenceJob;
import app.mikraal.activities.tasks.application.ReminderJob;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class RecurrenceAndRemindersTest extends IntegrationTest {

    @Autowired
    RecurrenceJob recurrence;

    @Autowired
    ReminderJob reminders;

    @Test
    void theNextInstanceIsCreatedWhenTheDeadlineArrivesUntilTheRecurrenceEnds() throws Exception {
        clock.set(Instant.parse("2027-03-01T09:00:00Z"));
        String event = UUID.randomUUID().toString();
        UUID tagId = id(body(get(JAVIER, "/groups/" + GROUP + "/task-tags")).get("items").get(0));
        JsonNode first = createTask(JAVIER, Map.of("title", "Revisar botiquín", "description", "Cada semana",
                "dueDate", "2027-03-03", "reminderDaysBefore", 1, "recurrence", Map.of("period", "weekly"),
                "owners", List.of(owner("position", SECRETARIA)), "tagIds", List.of(tagId),
                "links", List.of(Map.of("type", "event", "id", event))));
        String linked = tasks() + "?linked=event:" + event;

        clock.set(Instant.parse("2027-03-02T22:59:00Z"));
        recurrence.run(GROUP);
        assertThat(items(linked)).hasSize(1);

        clock.set(Instant.parse("2027-03-02T23:00:00Z"));
        recurrence.run(GROUP);
        recurrence.run(GROUP);
        List<JsonNode> instances = items(linked);
        assertThat(instances).hasSize(2);
        JsonNode next = instances.get(1);
        assertThat(next.get("dueDate").stringValue()).isEqualTo("2027-03-10");
        assertThat(next.get("status").stringValue()).isEqualTo("pending");
        for (String copied : List.of("title", "description", "owners", "tagIds", "links", "reminderDaysBefore")) {
            assertThat(next.get(copied)).as(copied).isEqualTo(first.get(copied));
        }
        assertThat(next.get("recurrence").get("seriesId")).isEqualTo(first.get("recurrence").get("seriesId"));
        assertThat(body(get(JAVIER, task(id(next)) + "/history")).get("items").get(0).get("kind").stringValue())
                .isEqualTo("instance_created");

        assertThat(post(JAVIER, task(id(first)) + "/recurrence/end", Map.of()).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(post(JAVIER, task(id(next)) + "/recurrence/end", Map.of()).getResponse().getStatus())
                .isEqualTo(409);
        assertThat(body(get(JAVIER, task(id(next)))).get("allowedActions").toString()).doesNotContain("end_recurrence");
        clock.set(Instant.parse("2027-03-20T09:00:00Z"));
        recurrence.run(GROUP);
        assertThat(items(linked)).hasSize(2);
    }

    @Test
    void theReminderGoesToTheOwnersOncePerDeadline() throws Exception {
        clock.set(Instant.parse("2027-04-01T09:00:00Z"));
        JsonNode task = createTask(JAVIER, Map.of("title", "Pagar el autobús", "dueDate", "2027-04-10",
                "owners", List.of(owner("position", SECRETARIA), owner("person", PABLO))));
        UUID id = id(task);

        clock.set(Instant.parse("2027-04-07T09:00:00Z"));
        reminders.run(GROUP);
        assertThat(reminders(id)).isEmpty();

        clock.set(Instant.parse("2027-04-08T09:00:00Z"));
        reminders.run(GROUP);
        reminders.run(GROUP);
        assertThat(reminders(id)).singleElement().satisfies(sent -> {
            assertThat(sent.dueDate()).isEqualTo(LocalDate.of(2027, 4, 10));
            assertThat(sent.recipients()).isEqualTo(Set.of(MARTA, PABLO));
        });

        patch(JAVIER, task(id), etag(task), Map.of("dueDate", "2027-04-20"));
        clock.set(Instant.parse("2027-04-18T09:00:00Z"));
        reminders.run(GROUP);
        assertThat(reminders(id)).extracting(RecordingNotifications.Sent::dueDate)
                .containsExactly(LocalDate.of(2027, 4, 10), LocalDate.of(2027, 4, 20));
    }

    @Test
    void pausedAndFinishedTasksGetNoReminder() throws Exception {
        clock.set(Instant.parse("2027-05-01T09:00:00Z"));
        Map<String, Object> body = Map.of("title", "x", "dueDate", "2027-05-03",
                "owners", List.of(owner("person", PABLO)));
        JsonNode paused = createTask(JAVIER, body);
        JsonNode finished = createTask(JAVIER, body);
        changeStatus(JAVIER, id(paused), etag(paused), Map.of("status", "paused", "pauseReason", "sin fecha"));
        changeStatus(JAVIER, id(finished), etag(finished), Map.of("status", "finished"));

        reminders.run(GROUP);

        assertThat(reminders(id(paused))).isEmpty();
        assertThat(reminders(id(finished))).isEmpty();
    }

    private List<JsonNode> items(String query) throws Exception {
        return StreamSupport.stream(body(get(JAVIER, query)).get("items").spliterator(), false).toList();
    }

    private List<RecordingNotifications.Sent> reminders(UUID taskId) {
        return notifications.about(taskId).stream().filter(s -> s.kind().equals("reminder")).toList();
    }
}
