package app.mikraal.activities.tasks.persistence;

import app.mikraal.activities.tasks.domain.RecurrencePeriod;
import app.mikraal.activities.tasks.domain.RecurrenceSeries;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SeriesRepository {

    private static final String SELECT =
            "SELECT id, group_id, period, anchor_due_date, last_due_date, active, created_by FROM recurrence_series";

    private final JdbcClient jdbc;

    SeriesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(RecurrenceSeries series, Instant at) {
        jdbc.sql("""
                INSERT INTO recurrence_series (id, group_id, period, anchor_due_date, last_due_date, active,
                                               created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .params(series.id(), series.groupId(), series.period().code(), series.anchorDueDate(),
                        series.lastDueDate(), series.active(), series.createdBy(), Timestamp.from(at))
                .update();
    }

    /** False when the series was already ended. */
    public boolean end(UUID id, UUID actor, Instant at) {
        return jdbc.sql("UPDATE recurrence_series SET active = false, ended_at = ?, ended_by = ? WHERE id = ? AND active")
                .params(Timestamp.from(at), actor, id).update() == 1;
    }

    /** Active series whose newest deadline has arrived. */
    public List<RecurrenceSeries> due(LocalDate today) {
        return jdbc.sql(SELECT + " WHERE active AND last_due_date <= ? ORDER BY id").param(today)
                .query((rs, n) -> new RecurrenceSeries(
                        rs.getObject("id", UUID.class),
                        rs.getObject("group_id", UUID.class),
                        RecurrencePeriod.fromCode(rs.getString("period")),
                        rs.getObject("anchor_due_date", LocalDate.class),
                        rs.getObject("last_due_date", LocalDate.class),
                        rs.getBoolean("active"),
                        rs.getObject("created_by", UUID.class)))
                .list();
    }

    public void advance(UUID id, LocalDate lastDueDate) {
        jdbc.sql("UPDATE recurrence_series SET last_due_date = ? WHERE id = ?").params(lastDueDate, id).update();
    }
}
