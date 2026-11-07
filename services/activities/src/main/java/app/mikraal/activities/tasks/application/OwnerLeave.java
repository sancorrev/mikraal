package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.authz.TaskRelations;
import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.Changes;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.HistoryKind;
import app.mikraal.activities.tasks.domain.Ids;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.VersionMismatchException;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * A person left the group: their open tasks lose them as a direct owner and may become unassigned. Tasks owned
 * through a position are untouched, since they follow the next holder. Internal: the people service will trigger
 * it with an event; there is no public endpoint.
 */
@Service
public class OwnerLeave {

    private final TenantScope scope;
    private final TaskRepository tasks;
    private final HistoryRepository history;
    private final TaskRelations relations;
    private final Clock clock;

    OwnerLeave(TenantScope scope, TaskRepository tasks, HistoryRepository history, TaskRelations relations,
               Clock clock) {
        this.scope = scope;
        this.tasks = tasks;
        this.history = history;
        this.relations = relations;
        this.clock = clock;
    }

    /** Returns the ids of the tasks that lost the person. {@code actor} records who processed the leave. */
    public List<UUID> removeFromOpenTasks(Actor actor, UUID groupId, UUID personId) {
        Owner leaving = Owner.person(personId);
        return scope.call(groupId, () -> {
            Instant now = clock.instant();
            List<Task> affected = tasks.openTasksOwnedByPerson(personId);
            for (Task task : affected) {
                Task.Draft draft = task.draft();
                draft.owners = task.owners().stream().filter(o -> !o.equals(leaving)).toList();
                Task next = draft.build(now);
                if (!tasks.update(next)) {
                    throw new VersionMismatchException();
                }
                history.append(new HistoryEntry(Ids.newId(), groupId, task.id(), now, actor.personId(), null,
                        HistoryKind.OWNER_REMOVED_ON_LEAVE, Changes.between(task, next).asMap()));
                relations.ownersChanged(task.id(), List.of(leaving), List.of());
            }
            return affected.stream().map(Task::id).toList();
        });
    }
}
