package app.mikraal.activities.tasks.persistence;

import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.HistoryKind;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Append-only: the app role cannot update or delete history, and a trigger rejects it for everyone. */
@Repository
public class HistoryRepository {

    /** Position of an entry in the history order: newest first. */
    public record PageKey(Instant at, UUID id) {
    }

    private static final TypeReference<Map<String, HistoryEntry.Change>> CHANGES = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    HistoryRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void append(HistoryEntry entry) {
        jdbc.sql("""
                INSERT INTO task_history (id, group_id, task_id, at, actor_person_id, on_behalf_of_position_id,
                                          kind, changes)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """)
                .params(entry.id(), entry.groupId(), entry.taskId(), Timestamp.from(entry.at()),
                        entry.actorPersonId(), entry.onBehalfOfPositionId(), entry.kind().code(),
                        entry.changes().isEmpty() ? null : json.writeValueAsString(entry.changes()))
                .update();
    }

    public List<HistoryEntry> page(UUID taskId, PageKey after, int limit) {
        String afterClause = after == null ? "" : " AND (at, id) < (:afterAt, :afterId)";
        var statement = jdbc.sql("""
                SELECT id, group_id, task_id, at, actor_person_id, on_behalf_of_position_id, kind, changes::text
                FROM task_history WHERE task_id = :taskId""" + afterClause + " ORDER BY at DESC, id DESC LIMIT :limit")
                .param("taskId", taskId)
                .param("limit", limit);
        if (after != null) {
            statement = statement.param("afterAt", Timestamp.from(after.at())).param("afterId", after.id());
        }
        return statement.query((rs, n) -> {
            String changes = rs.getString("changes");
            return new HistoryEntry(
                    rs.getObject("id", UUID.class),
                    rs.getObject("group_id", UUID.class),
                    rs.getObject("task_id", UUID.class),
                    rs.getTimestamp("at").toInstant(),
                    rs.getObject("actor_person_id", UUID.class),
                    rs.getObject("on_behalf_of_position_id", UUID.class),
                    HistoryKind.fromCode(rs.getString("kind")),
                    changes == null ? Map.of() : json.readValue(changes, CHANGES));
        }).list();
    }
}
