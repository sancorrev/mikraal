package app.mikraal.activities.tasks.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** One append-only history record. {@code changes} maps a field to its plain JSON {@code from} and {@code to}. */
public record HistoryEntry(
        UUID id,
        UUID groupId,
        UUID taskId,
        Instant at,
        UUID actorPersonId,
        UUID onBehalfOfPositionId,
        HistoryKind kind,
        Map<String, Change> changes) {

    public record Change(Object from, Object to) {
    }

    public HistoryEntry {
        changes = Map.copyOf(changes);
    }
}
