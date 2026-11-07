package app.mikraal.activities.tasks.application;

import static app.mikraal.activities.tasks.authz.GroupAction.CREATE_TASKS;
import static app.mikraal.activities.tasks.authz.GroupAction.MANAGE_TAGS;
import static app.mikraal.activities.tasks.authz.GroupAction.VIEW_TASKS;

import app.mikraal.activities.tasks.authz.Authorizer;
import app.mikraal.activities.tasks.authz.GroupAction;
import app.mikraal.activities.tasks.authz.PositionHolders;
import app.mikraal.activities.tasks.calendar.GroupClock;
import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.NotFoundException;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.TaskFilter;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Reads of the tasks module. Every scouter of the group sees every task. */
@Service
public class TaskQueries {

    private final TenantScope scope;
    private final TaskRepository tasks;
    private final HistoryRepository history;
    private final Authorizer authorizer;
    private final PositionHolders holders;
    private final TaskViews views;
    private final GroupClock clock;

    TaskQueries(TenantScope scope, TaskRepository tasks, HistoryRepository history, Authorizer authorizer,
                PositionHolders holders, TaskViews views, GroupClock clock) {
        this.scope = scope;
        this.tasks = tasks;
        this.history = history;
        this.authorizer = authorizer;
        this.holders = holders;
        this.views = views;
        this.clock = clock;
    }

    public Page<TaskView> list(Actor actor, UUID groupId, TaskListQuery query) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        var filter = new TaskFilter(query.statuses(), ownerFilter(actor, query.assignedTo()), query.tagId(),
                query.linked(), query.dueFrom(), query.dueTo(), query.overdue(), clock.today(groupId));
        TaskRepository.PageKey after = Cursors.decodeTask(query.cursor());
        List<Task> found = scope.call(groupId, () -> tasks.page(filter, after, query.limit() + 1));
        List<Task> page = found.stream().limit(query.limit()).toList();
        String next = found.size() > query.limit()
                ? Cursors.encode(new TaskRepository.PageKey(page.getLast().dueDate(), page.getLast().id()))
                : null;
        return new Page<>(views.of(actor, groupId, page), next);
    }

    public TaskView get(Actor actor, UUID groupId, UUID taskId) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        Task task = scope.call(groupId, () -> tasks.find(taskId)).orElseThrow(() -> new NotFoundException("Task"));
        return views.of(actor, task);
    }

    public Page<HistoryEntry> history(Actor actor, UUID groupId, UUID taskId, String cursor, int limit) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        HistoryRepository.PageKey after = Cursors.decodeHistory(cursor);
        List<HistoryEntry> found = scope.call(groupId, () -> {
            if (tasks.find(taskId).isEmpty()) {
                throw new NotFoundException("Task");
            }
            return history.page(taskId, after, limit + 1);
        });
        List<HistoryEntry> page = found.stream().limit(limit).toList();
        String next = found.size() > limit
                ? Cursors.encode(new HistoryRepository.PageKey(page.getLast().at(), page.getLast().id()))
                : null;
        return new Page<>(page, next);
    }

    public Set<GroupAction> capabilities(Actor actor, UUID groupId) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        return authorizer.allowed(actor, groupId, Set.of(CREATE_TASKS, MANAGE_TAGS));
    }

    private TaskFilter.OwnerFilter ownerFilter(Actor actor, TaskListQuery.Assignee assignee) {
        return switch (assignee) {
            case null -> null;
            case TaskListQuery.Me me -> personAndPositions(actor.personId());
            case TaskListQuery.Person person -> personAndPositions(person.id());
            case TaskListQuery.Position position -> new TaskFilter.AnyOwner(Set.of(Owner.position(position.id())));
            case TaskListQuery.Unassigned unassigned -> new TaskFilter.Unassigned();
        };
    }

    private TaskFilter.AnyOwner personAndPositions(UUID personId) {
        Set<Owner> owners = new HashSet<>();
        owners.add(Owner.person(personId));
        holders.positionsHeldBy(personId).forEach(id -> owners.add(Owner.position(id)));
        return new TaskFilter.AnyOwner(owners);
    }
}
