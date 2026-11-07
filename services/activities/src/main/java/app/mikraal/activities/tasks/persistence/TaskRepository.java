package app.mikraal.activities.tasks.persistence;

import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Recurrence;
import app.mikraal.activities.tasks.domain.RecurrencePeriod;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tasks and their owners, links and tags. Runs inside a {@link TenantScope}: RLS limits it to one group. */
@Repository
public class TaskRepository {

    /** Position of a task in the list order: deadline (none = last), then id. */
    public record PageKey(LocalDate dueDate, UUID id) {
    }

    private static final String SELECT = """
            SELECT t.id, t.group_id, t.title, t.description, t.status, t.pause_reason, t.due_date,
                   t.reminder_days_before, t.series_id, s.period, s.active, t.created_by, t.created_at,
                   t.updated_at, t.version
            FROM task t LEFT JOIN recurrence_series s ON s.id = t.series_id
            """;

    private final JdbcClient jdbc;

    TaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Task task) {
        jdbc.sql("""
                INSERT INTO task (id, group_id, title, description, status, pause_reason, due_date,
                                  reminder_days_before, series_id, created_by, created_at, updated_at, version)
                VALUES (:id, :groupId, :title, :description, :status, :pauseReason, :dueDate,
                        :reminder, :seriesId, :createdBy, :createdAt, :updatedAt, :version)
                """)
                .params(fields(task))
                .param("seriesId", task.recurrence() == null ? null : task.recurrence().seriesId())
                .param("createdBy", task.createdBy())
                .param("createdAt", Timestamp.from(task.createdAt()))
                .update();
        insertChildren(task);
    }

    /** Writes the next version; false when the stored version is no longer the one before it. */
    public boolean update(Task task) {
        int updated = jdbc.sql("""
                UPDATE task SET title = :title, description = :description, status = :status,
                                pause_reason = :pauseReason, due_date = :dueDate, reminder_days_before = :reminder,
                                updated_at = :updatedAt, version = :version
                WHERE id = :id AND version = :version - 1 AND deleted_at IS NULL
                """)
                .params(fields(task))
                .update();
        if (updated == 0) {
            return false;
        }
        for (String table : List.of("task_owner", "task_link", "task_tag")) {
            jdbc.sql("DELETE FROM " + table + " WHERE task_id = ?").param(task.id()).update();
        }
        insertChildren(task);
        return true;
    }

    /** Keeps a tombstone for sync; the content is never returned again. */
    public boolean markDeleted(UUID id, int version, UUID actor, Instant at) {
        return jdbc.sql("""
                UPDATE task SET deleted_at = :at, deleted_by = :actor, updated_at = :at, version = version + 1
                WHERE id = :id AND version = :version AND deleted_at IS NULL
                """)
                .param("id", id).param("version", version).param("actor", actor).param("at", Timestamp.from(at))
                .update() == 1;
    }

    public Optional<Task> find(UUID id) {
        return load(jdbc.sql(SELECT + " WHERE t.id = ? AND t.deleted_at IS NULL").param(id)).stream().findFirst();
    }

    /** Whether the id was ever used in this group, deleted tasks included. */
    public boolean exists(UUID id) {
        return jdbc.sql("SELECT count(*) FROM task WHERE id = ?").param(id).query(Long.class).single() > 0;
    }

    /** Up to {@code limit} tasks after {@code after} (null = from the start), in list order. */
    public List<Task> page(TaskFilter filter, PageKey after, int limit) {
        var sql = new StringBuilder(SELECT).append(" WHERE t.deleted_at IS NULL");
        var params = new HashMap<String, Object>();
        if (filter.statuses() != null && !filter.statuses().isEmpty()) {
            sql.append(" AND t.status IN (:statuses)");
            params.put("statuses", filter.statuses().stream().map(TaskStatus::code).toList());
        }
        switch (filter.owner()) {
            case null -> { }
            case TaskFilter.Unassigned u -> sql.append(" AND NOT EXISTS (SELECT 1 FROM task_owner o WHERE o.task_id = t.id)");
            case TaskFilter.AnyOwner any -> appendOwners(sql, params, any.owners());
        }
        if (filter.tagId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM task_tag g WHERE g.task_id = t.id AND g.tag_id = :tagId)");
            params.put("tagId", filter.tagId());
        }
        if (filter.linked() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM task_link l WHERE l.task_id = t.id"
                    + " AND l.link_type = :linkType AND l.link_id = :linkId)");
            params.put("linkType", filter.linked().type().code());
            params.put("linkId", filter.linked().id());
        }
        if (filter.dueFrom() != null) {
            sql.append(" AND t.due_date >= :dueFrom");
            params.put("dueFrom", filter.dueFrom());
        }
        if (filter.dueTo() != null) {
            sql.append(" AND t.due_date <= :dueTo");
            params.put("dueTo", filter.dueTo());
        }
        if (filter.overdue() != null) {
            String overdue = "(t.status <> 'finished' AND t.due_date IS NOT NULL AND t.due_date < :today)";
            sql.append(filter.overdue() ? " AND " + overdue : " AND NOT " + overdue);
            params.put("today", filter.today());
        }
        if (after != null) {
            sql.append(" AND (coalesce(t.due_date, 'infinity'::date), t.id)"
                    + " > (coalesce(:afterDue, 'infinity'::date), :afterId)");
            params.put("afterDue", after.dueDate());
            params.put("afterId", after.id());
        }
        sql.append(" ORDER BY coalesce(t.due_date, 'infinity'::date), t.id LIMIT :limit");
        params.put("limit", limit);
        return load(jdbc.sql(sql.toString()).params(params));
    }

    /** Open, not paused tasks whose reminder day has come and whose current deadline was not reminded yet. */
    public List<Task> dueForReminder(LocalDate today) {
        return load(jdbc.sql(SELECT + """
                 WHERE t.deleted_at IS NULL AND t.status IN ('pending', 'in_progress')
                   AND t.reminder_days_before IS NOT NULL
                   AND t.due_date >= :today AND t.due_date - t.reminder_days_before <= :today
                   AND t.reminder_sent_for IS DISTINCT FROM t.due_date
                """).param("today", today));
    }

    public void markReminderSent(UUID id, LocalDate dueDate) {
        jdbc.sql("UPDATE task SET reminder_sent_for = ? WHERE id = ?").param(dueDate).param(id).update();
    }

    public List<Task> openTasksOwnedByPerson(UUID personId) {
        return load(jdbc.sql(SELECT + """
                 WHERE t.deleted_at IS NULL AND t.status <> 'finished'
                   AND EXISTS (SELECT 1 FROM task_owner o WHERE o.task_id = t.id
                               AND o.owner_type = 'person' AND o.owner_id = ?)
                """).param(personId));
    }

    /** The newest live instance of a series: the one the next instance copies. */
    public Optional<Task> latestInSeries(UUID seriesId) {
        return load(jdbc.sql(SELECT + " WHERE t.series_id = ? AND t.deleted_at IS NULL"
                + " ORDER BY t.due_date DESC, t.id DESC LIMIT 1").param(seriesId)).stream().findFirst();
    }

    public List<Task> carryingTag(UUID tagId) {
        return load(jdbc.sql(SELECT + " WHERE t.deleted_at IS NULL"
                + " AND EXISTS (SELECT 1 FROM task_tag g WHERE g.task_id = t.id AND g.tag_id = ?)").param(tagId));
    }

    /** Detaches a tag from every task, deleted ones included, so the tag can leave the catalogue. */
    public void detachTag(UUID tagId) {
        jdbc.sql("DELETE FROM task_tag WHERE tag_id = ?").param(tagId).update();
    }

    private static void appendOwners(StringBuilder sql, Map<String, Object> params, Set<Owner> owners) {
        List<String> clauses = new ArrayList<>();
        for (Owner.Type type : Owner.Type.values()) {
            List<UUID> ids = owners.stream().filter(o -> o.type() == type).map(Owner::id).toList();
            if (!ids.isEmpty()) {
                clauses.add("(o.owner_type = '" + type.code() + "' AND o.owner_id IN (:" + type.code() + "Ids))");
                params.put(type.code() + "Ids", ids);
            }
        }
        if (clauses.isEmpty()) {
            sql.append(" AND false");
            return;
        }
        sql.append(" AND EXISTS (SELECT 1 FROM task_owner o WHERE o.task_id = t.id AND (")
                .append(String.join(" OR ", clauses)).append("))");
    }

    private static Map<String, Object> fields(Task task) {
        var fields = new HashMap<String, Object>();
        fields.put("id", task.id());
        fields.put("groupId", task.groupId());
        fields.put("title", task.title());
        fields.put("description", task.description());
        fields.put("status", task.status().code());
        fields.put("pauseReason", task.pauseReason());
        fields.put("dueDate", task.dueDate());
        fields.put("reminder", task.reminderDaysBefore());
        fields.put("updatedAt", Timestamp.from(task.updatedAt()));
        fields.put("version", task.version());
        return fields;
    }

    private void insertChildren(Task task) {
        for (Owner owner : task.owners()) {
            jdbc.sql("INSERT INTO task_owner (group_id, task_id, owner_type, owner_id) VALUES (?, ?, ?, ?)")
                    .params(task.groupId(), task.id(), owner.type().code(), owner.id()).update();
        }
        for (Link link : task.links()) {
            jdbc.sql("INSERT INTO task_link (group_id, task_id, link_type, link_id) VALUES (?, ?, ?, ?)")
                    .params(task.groupId(), task.id(), link.type().code(), link.id()).update();
        }
        for (UUID tagId : task.tagIds()) {
            jdbc.sql("INSERT INTO task_tag (group_id, task_id, tag_id) VALUES (?, ?, ?)")
                    .params(task.groupId(), task.id(), tagId).update();
        }
    }

    private List<Task> load(JdbcClient.StatementSpec statement) {
        List<TaskRow> rows = statement.query(TaskRepository::row).list();
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = rows.stream().map(TaskRow::id).toList();
        Map<UUID, List<Owner>> owners = children("SELECT task_id, owner_type, owner_id FROM task_owner", ids,
                rs -> new Owner(Owner.Type.fromCode(rs.getString(2)), rs.getObject(3, UUID.class)));
        Map<UUID, List<Link>> links = children("SELECT task_id, link_type, link_id FROM task_link", ids,
                rs -> new Link(Link.Type.fromCode(rs.getString(2)), rs.getObject(3, UUID.class)));
        Map<UUID, List<UUID>> tags = children("SELECT task_id, tag_id FROM task_tag", ids,
                rs -> rs.getObject(2, UUID.class));
        return rows.stream().map(r -> r.toTask(
                owners.getOrDefault(r.id(), List.of()),
                tags.getOrDefault(r.id(), List.of()),
                links.getOrDefault(r.id(), List.of()))).toList();
    }

    private interface ColumnReader<T> {
        T read(ResultSet rs) throws SQLException;
    }

    private <T> Map<UUID, List<T>> children(String select, Collection<UUID> taskIds, ColumnReader<T> reader) {
        record Child<T>(UUID taskId, T value) {
        }
        return jdbc.sql(select + " WHERE task_id IN (:ids)").param("ids", taskIds)
                .query((rs, n) -> new Child<>(rs.getObject(1, UUID.class), reader.read(rs)))
                .list().stream()
                .collect(Collectors.groupingBy(Child::taskId, Collectors.mapping(Child::value, Collectors.toList())));
    }

    private record TaskRow(UUID id, UUID groupId, String title, String description, TaskStatus status,
                           String pauseReason, LocalDate dueDate, Integer reminder, Recurrence recurrence,
                           UUID createdBy, Instant createdAt, Instant updatedAt, int version) {

        Task toTask(List<Owner> owners, List<UUID> tagIds, List<Link> links) {
            return new Task(id, groupId, title, description, status, pauseReason, owners, dueDate, reminder,
                    tagIds, links, recurrence, createdBy, createdAt, updatedAt, version);
        }
    }

    private static TaskRow row(ResultSet rs, int rowNum) throws SQLException {
        UUID seriesId = rs.getObject("series_id", UUID.class);
        Recurrence recurrence = seriesId == null ? null
                : new Recurrence(seriesId, RecurrencePeriod.fromCode(rs.getString("period")), rs.getBoolean("active"));
        return new TaskRow(
                rs.getObject("id", UUID.class),
                rs.getObject("group_id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                TaskStatus.fromCode(rs.getString("status")),
                rs.getString("pause_reason"),
                rs.getObject("due_date", LocalDate.class),
                rs.getObject("reminder_days_before", Integer.class),
                recurrence,
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getInt("version"));
    }
}
