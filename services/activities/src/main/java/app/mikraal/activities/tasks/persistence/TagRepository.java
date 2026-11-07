package app.mikraal.activities.tasks.persistence;

import app.mikraal.activities.tasks.domain.ConflictException;
import app.mikraal.activities.tasks.domain.Tag;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The group's tag catalogue. Names are unique per group, ignoring case. */
@Repository
public class TagRepository {

    private final JdbcClient jdbc;

    TagRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Tag> all() {
        return jdbc.sql("SELECT id, group_id, name FROM tag ORDER BY lower(name), id").query(Tag.class).list();
    }

    public Optional<Tag> find(UUID id) {
        return jdbc.sql("SELECT id, group_id, name FROM tag WHERE id = ?").param(id).query(Tag.class).optional();
    }

    public boolean nameExists(String name) {
        return jdbc.sql("SELECT count(*) FROM tag WHERE lower(name) = lower(?)").param(name)
                .query(Long.class).single() > 0;
    }

    public Set<UUID> existing(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql("SELECT id FROM tag WHERE id IN (:ids)").param("ids", ids)
                .query(UUID.class).list());
    }

    public void insert(Tag tag, Instant at) {
        try {
            jdbc.sql("INSERT INTO tag (id, group_id, name, created_at) VALUES (?, ?, ?, ?)")
                    .params(tag.id(), tag.groupId(), tag.name(), Timestamp.from(at)).update();
        } catch (DuplicateKeyException e) {
            throw ConflictException.duplicateTag();
        }
    }

    public void rename(UUID id, String name) {
        try {
            jdbc.sql("UPDATE tag SET name = ? WHERE id = ?").params(name, id).update();
        } catch (DuplicateKeyException e) {
            throw ConflictException.duplicateTag();
        }
    }

    public void delete(UUID id) {
        jdbc.sql("DELETE FROM tag WHERE id = ?").param(id).update();
    }
}
