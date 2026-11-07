package app.mikraal.bff.web.tasks;

import static app.mikraal.bff.web.SeedIds.COORDINATION;
import static app.mikraal.bff.web.SeedIds.EVENT;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static app.mikraal.bff.web.SeedIds.MARTA;
import static app.mikraal.bff.web.tasks.NamesFixture.TAG_CAMP;
import static app.mikraal.bff.web.tasks.NamesFixture.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntry;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntryActor;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntryChangesValue;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.TaskHistoryEntry;
import app.mikraal.bff.web.api.model.TaskHistoryEntryChangesValue;
import org.junit.jupiter.api.Test;

class HistoryMapperTest {

    @Test
    void resolvesTheActorAndThePositionActedAs() {
        TaskHistoryEntry entry = HistoryMapper.toEntry(
                entry(new ActivitiesTaskHistoryEntryActor(LUCIA).onBehalfOfPositionId(COORDINATION), Map.of()),
                NamesFixture.names());

        assertThat(entry.getActor()).isEqualTo(new NamedRef(LUCIA, "Lucía Martín Ortega"));
        assertThat(entry.getOnBehalfOfPosition()).isEqualTo(new NamedRef(COORDINATION, "Coordinación de grupo"));
        assertThat(entry.getKind()).isEqualTo(TaskHistoryEntry.KindEnum.EDITED);
    }

    @Test
    void leavesThePositionEmptyWhenTheActorActedAsThemself() {
        TaskHistoryEntry entry = HistoryMapper.toEntry(
                entry(new ActivitiesTaskHistoryEntryActor(UNKNOWN), Map.of()), NamesFixture.names());

        assertThat(entry.getActor().getName()).isEqualTo("Desconocido");
        assertThat(entry.getOnBehalfOfPosition()).isNull();
    }

    @Test
    void resolvesIdsInsideChanges() {
        Map<String, ActivitiesTaskHistoryEntryChangesValue> changes = Map.of(
                "owners", change(
                        List.of(Map.of("type", "person", "id", MARTA.toString())),
                        List.of(Map.of("type", "position", "id", COORDINATION.toString()),
                                Map.of("type", "person", "id", UNKNOWN.toString()))),
                "tagIds", change(List.of(), List.of(TAG_CAMP.toString(), UNKNOWN.toString())),
                "links", change(null, List.of(Map.of("type", "event", "id", EVENT.toString()))),
                "title", change("Seguro", "Renovar el seguro"));

        Map<String, TaskHistoryEntryChangesValue> resolved = HistoryMapper.toEntry(
                entry(new ActivitiesTaskHistoryEntryActor(LUCIA), changes), NamesFixture.names()).getChanges();

        assertThat(resolved.get("owners").getFrom()).isEqualTo(List.of(
                Map.of("type", "person", "id", MARTA.toString(), "name", "Marta Gómez Vidal")));
        assertThat(resolved.get("owners").getTo()).isEqualTo(List.of(
                Map.of("type", "position", "id", COORDINATION.toString(), "name", "Coordinación de grupo"),
                Map.of("type", "person", "id", UNKNOWN.toString(), "name", "Desconocido")));
        assertThat(resolved.get("tagIds").getTo()).isEqualTo(List.of(
                Map.of("id", TAG_CAMP.toString(), "name", "Campamento"),
                Map.of("id", UNKNOWN.toString(), "name", "Desconocido")));
        assertThat(resolved.get("links").getFrom()).isNull();
        assertThat(resolved.get("links").getTo()).isEqualTo(List.of(
                Map.of("type", "event", "id", EVENT.toString(), "name", "Salida de manada a la sierra")));
        assertThat(resolved.get("title").getTo()).isEqualTo("Renovar el seguro");
    }

    @Test
    void leavesUnrecognisedShapesAsTheyAre() {
        Map<String, ActivitiesTaskHistoryEntryChangesValue> changes = Map.of(
                "owners", change("not-a-list-of-refs", List.of(Map.of("type", "person", "id", "not-a-uuid"))),
                "tagIds", change(List.of(42), null));

        Map<String, TaskHistoryEntryChangesValue> resolved = HistoryMapper.toEntry(
                entry(new ActivitiesTaskHistoryEntryActor(LUCIA), changes), NamesFixture.names()).getChanges();

        assertThat(resolved.get("owners").getFrom()).isEqualTo("not-a-list-of-refs");
        assertThat(resolved.get("owners").getTo()).isEqualTo(List.of(Map.of("type", "person", "id", "not-a-uuid")));
        assertThat(resolved.get("tagIds").getFrom()).isEqualTo(List.of(42));
        assertThat(resolved.get("tagIds").getTo()).isNull();
    }

    private static ActivitiesTaskHistoryEntry entry(ActivitiesTaskHistoryEntryActor actor,
            Map<String, ActivitiesTaskHistoryEntryChangesValue> changes) {
        return new ActivitiesTaskHistoryEntry()
                .id(UUID.randomUUID())
                .at(OffsetDateTime.parse("2026-11-07T10:00:00Z"))
                .actor(actor)
                .kind(ActivitiesTaskHistoryEntry.KindEnum.EDITED)
                .changes(changes);
    }

    private static ActivitiesTaskHistoryEntryChangesValue change(Object from, Object to) {
        return new ActivitiesTaskHistoryEntryChangesValue().from(from).to(to);
    }
}
