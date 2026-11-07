package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.GROUP;
import static app.mikraal.activities.tasks.Seed.JAVIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.mikraal.activities.tasks.persistence.TenantScope;
import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.ClientTupleKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Group isolation is enforced by PostgreSQL, not by the queries: the SQL below has no group filter. */
class RowLevelSecurityTest extends IntegrationTest {

    @Autowired
    TenantScope scope;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    OpenFgaClient openFga;

    private final UUID otherGroup = UUID.randomUUID();
    private final UUID otherScouter = UUID.randomUUID();
    private UUID ownTask;
    private UUID otherTask;

    @BeforeEach
    void twoGroupsWithATaskEach() throws Exception {
        openFga.writeTuples(List.of(
                new ClientTupleKey().user("person:" + otherScouter).relation("scouter")._object("group:" + otherGroup),
                new ClientTupleKey().user("group:" + otherGroup + "#scouter").relation("task_creator")
                        ._object("group:" + otherGroup))).get();
        ownTask = id(createTask(JAVIER, Map.of("title", "Grupo A")));
        var created = post(otherScouter, "/groups/" + otherGroup + "/tasks", Map.of("title", "Grupo B"));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        otherTask = id(body(created));
    }

    @Test
    void aGroupNeverSeesAnotherGroupsRowsEvenWithoutAFilter() {
        for (String table : List.of("task", "task_history")) {
            List<UUID> groups = scope.call(GROUP,
                    () -> jdbc.sql("SELECT DISTINCT group_id FROM " + table).query(UUID.class).list());
            assertThat(groups).as(table).containsExactly(GROUP);
        }
        assertThat(scope.call(GROUP, () -> count("SELECT count(*) FROM task WHERE id = ?", otherTask))).isZero();
        assertThat(scope.call(otherGroup, () -> count("SELECT count(*) FROM task WHERE id = ?", ownTask))).isZero();
        assertThat(scope.call(otherGroup, () -> count("SELECT count(*) FROM task", null))).isEqualTo(1);
    }

    @Test
    void withoutAGroupNothingIsVisible() {
        assertThat(count("SELECT count(*) FROM task", null)).isZero();
    }

    @Test
    void rowsOfAnotherGroupCannotBeWrittenOrChanged() {
        assertThatThrownBy(() -> scope.run(GROUP, () -> jdbc.sql("""
                INSERT INTO tag (id, group_id, name, created_at) VALUES (?, ?, 'Intrusa', now())
                """).params(UUID.randomUUID(), otherGroup).update())).isInstanceOf(DataAccessException.class);
        int updated = scope.call(GROUP, () -> jdbc.sql("UPDATE task SET title = 'cambiado' WHERE id = ?")
                .param(otherTask).update());
        assertThat(updated).isZero();
    }

    @Test
    void historyIsAppendOnlyForTheApplication() {
        assertThatThrownBy(() -> scope.run(GROUP, () -> jdbc.sql("UPDATE task_history SET kind = 'edited'")
                .update())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> scope.run(GROUP, () -> jdbc.sql("DELETE FROM task_history").update()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void theApiOfOneGroupDoesNotReachTheOther() throws Exception {
        assertThat(get(otherScouter, "/groups/" + otherGroup + "/tasks/" + ownTask).getResponse().getStatus())
                .isEqualTo(404);
        assertThat(get(otherScouter, task(ownTask)).getResponse().getStatus()).isEqualTo(403);
        assertThat(body(get(otherScouter, "/groups/" + otherGroup + "/tasks")).get("items").size()).isEqualTo(1);
    }

    private long count(String sql, UUID param) {
        var statement = jdbc.sql(sql);
        return (param == null ? statement : statement.param(param)).query(Long.class).single();
    }
}
