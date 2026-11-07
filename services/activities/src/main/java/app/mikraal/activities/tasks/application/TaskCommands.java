package app.mikraal.activities.tasks.application;

import static app.mikraal.activities.tasks.authz.GroupAction.CREATE_TASKS;
import static app.mikraal.activities.tasks.authz.GroupAction.VIEW_TASKS;

import app.mikraal.activities.tasks.authz.Authorizer;
import app.mikraal.activities.tasks.authz.PositionHolders;
import app.mikraal.activities.tasks.authz.TaskRelations;
import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.Changes;
import app.mikraal.activities.tasks.domain.ConflictException;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.HistoryKind;
import app.mikraal.activities.tasks.domain.Ids;
import app.mikraal.activities.tasks.domain.NotFoundException;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Recurrence;
import app.mikraal.activities.tasks.domain.RecurrenceSeries;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskAction;
import app.mikraal.activities.tasks.domain.TaskStatus;
import app.mikraal.activities.tasks.domain.ValidationException;
import app.mikraal.activities.tasks.domain.VersionMismatchException;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.SeriesRepository;
import app.mikraal.activities.tasks.persistence.TagRepository;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** Every write on a task: explicit commands with an actor (ARQ-01, group 2). */
@Service
public class TaskCommands {

    public record Created(TaskView view, boolean isNew) {
    }

    private record Outcome(Task before, Task after) {
    }

    private final TenantScope scope;
    private final TaskRepository tasks;
    private final SeriesRepository series;
    private final HistoryRepository history;
    private final TagRepository tags;
    private final Authorizer authorizer;
    private final TaskRelations relations;
    private final PositionHolders holders;
    private final Assignments assignments;
    private final TaskViews views;
    private final Clock clock;

    TaskCommands(TenantScope scope, TaskRepository tasks, SeriesRepository series, HistoryRepository history,
                 TagRepository tags, Authorizer authorizer, TaskRelations relations, PositionHolders holders,
                 Assignments assignments, TaskViews views, Clock clock) {
        this.scope = scope;
        this.tasks = tasks;
        this.series = series;
        this.history = history;
        this.tags = tags;
        this.authorizer = authorizer;
        this.relations = relations;
        this.holders = holders;
        this.assignments = assignments;
        this.views = views;
        this.clock = clock;
    }

    /** Idempotent on {@code command.id()}: a repeated id returns the stored task. */
    public Created create(Actor actor, UUID groupId, CreateTaskCommand command) {
        authorizer.require(actor, groupId, CREATE_TASKS);
        validate(command);
        UUID id = command.id() != null ? command.id() : Ids.newId();
        Outcome outcome;
        try {
            outcome = scope.call(groupId, () -> tasks.find(id)
                    .map(existing -> new Outcome(existing, existing))
                    .orElseGet(() -> new Outcome(null, insert(actor, groupId, id, command))));
        } catch (DuplicateKeyException concurrentCreate) {
            outcome = scope.call(groupId, () -> tasks.find(id).map(t -> new Outcome(t, t))
                    .orElseThrow(ConflictException::idInUse));
        }
        Task task = outcome.after();
        boolean isNew = outcome.before() == null;
        if (isNew) {
            assignments.notifyNewOwners(groupId, task.id(), task.owners());
        }
        return new Created(views.of(actor, task), isNew);
    }

    public TaskView update(Actor actor, UUID groupId, UUID taskId, int expectedVersion, TaskPatch patch) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        Outcome outcome = scope.call(groupId, () -> {
            Task current = load(taskId, expectedVersion, actor, TaskAction.EDIT);
            Task next = apply(current, patch);
            Changes changes = Changes.between(current, next);
            if (changes.isEmpty()) {
                return new Outcome(current, current);
            }
            requireKnownTags(next.tagIds());
            save(next);
            history.append(entry(next, actor, null, HistoryKind.EDITED, changes));
            relations.ownersChanged(taskId, minus(current.owners(), next.owners()),
                    minus(next.owners(), current.owners()));
            return new Outcome(current, next);
        });
        assignments.notifyNewOwners(groupId, taskId, minus(outcome.after().owners(), outcome.before().owners()));
        return views.of(actor, outcome.after());
    }

    public TaskView changeStatus(Actor actor, UUID groupId, UUID taskId, int expectedVersion, TaskStatus target,
                                 String pauseReason) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        boolean paused = target == TaskStatus.PAUSED;
        if (paused && (pauseReason == null || pauseReason.isBlank())) {
            throw ValidationException.of("pauseReason", "pause_reason_required");
        }
        if (!paused && pauseReason != null) {
            throw ValidationException.of("pauseReason", "pause_reason_not_allowed");
        }
        Task next = scope.call(groupId, () -> {
            Task current = load(taskId, expectedVersion, actor, TaskAction.CHANGE_STATUS);
            if (!current.status().canMoveTo(target)) {
                throw ConflictException.invalidTransition(current.status(), target);
            }
            Task.Draft draft = current.draft();
            draft.status = target;
            draft.pauseReason = paused ? pauseReason.strip() : null;
            Task changed = draft.build(clock.instant());
            save(changed);
            history.append(entry(changed, actor, positionActedAs(actor, current), HistoryKind.STATUS_CHANGED,
                    Changes.between(current, changed)));
            return changed;
        });
        return views.of(actor, next);
    }

    public void delete(Actor actor, UUID groupId, UUID taskId, int expectedVersion) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        scope.run(groupId, () -> {
            Task current = load(taskId, expectedVersion, actor, TaskAction.DELETE);
            if (!tasks.markDeleted(taskId, expectedVersion, actor.personId(), clock.instant())) {
                throw new VersionMismatchException();
            }
            relations.deleted(current);
        });
    }

    public void endRecurrence(Actor actor, UUID groupId, UUID taskId) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        scope.run(groupId, () -> {
            Task current = tasks.find(taskId).orElseThrow(() -> new NotFoundException("Task"));
            authorizer.require(actor, current, TaskAction.END_RECURRENCE);
            if (!current.hasActiveRecurrence()) {
                throw ConflictException.noActiveRecurrence();
            }
            Instant now = clock.instant();
            Recurrence recurrence = current.recurrence();
            series.end(recurrence.seriesId(), actor.personId(), now);
            Task.Draft draft = current.draft();
            draft.recurrence = new Recurrence(recurrence.seriesId(), recurrence.period(), false);
            Task ended = draft.build(now);
            save(ended);
            history.append(entry(ended, actor, null, HistoryKind.RECURRENCE_ENDED,
                    new Changes().track("recurrenceActive", true, false)));
        });
    }

    private Task insert(Actor actor, UUID groupId, UUID id, CreateTaskCommand command) {
        if (tasks.exists(id)) {
            throw ConflictException.idInUse();
        }
        requireKnownTags(command.tagIds());
        Instant now = clock.instant();
        Recurrence recurrence = null;
        if (command.recurrence() != null) {
            UUID seriesId = Ids.newId();
            series.insert(new RecurrenceSeries(seriesId, groupId, command.recurrence(), command.dueDate(),
                    command.dueDate(), true, actor.personId()), now);
            recurrence = new Recurrence(seriesId, command.recurrence(), true);
        }
        Integer reminder = command.dueDate() != null && command.reminderDaysBefore() == null
                ? Integer.valueOf(Task.DEFAULT_REMINDER_DAYS) : command.reminderDaysBefore();
        Task task = new Task(id, groupId, command.title().strip(), command.description(), TaskStatus.PENDING, null,
                command.owners(), command.dueDate(), reminder, command.tagIds(), command.links(), recurrence,
                actor.personId(), now, now, 1);
        tasks.insert(task);
        history.append(entry(task, actor, null, HistoryKind.CREATED, Changes.created(task)));
        relations.created(task);
        return task;
    }

    private Task load(UUID taskId, int expectedVersion, Actor actor, TaskAction action) {
        Task current = tasks.find(taskId).orElseThrow(() -> new NotFoundException("Task"));
        authorizer.require(actor, current, action);
        if (current.version() != expectedVersion) {
            throw new VersionMismatchException();
        }
        return current;
    }

    private void save(Task next) {
        if (!tasks.update(next)) {
            throw new VersionMismatchException();
        }
    }

    private Task apply(Task current, TaskPatch patch) {
        List<ValidationException.FieldError> errors = new ArrayList<>();
        Task.Draft draft = current.draft();
        if (patch.title() != null) {
            draft.title = patch.title().value().strip();
        }
        if (patch.description() != null) {
            draft.description = patch.description().value();
        }
        if (patch.owners() != null) {
            draft.owners = patch.owners().value();
        }
        if (patch.dueDate() != null) {
            draft.dueDate = patch.dueDate().value();
            if (draft.dueDate == null) {
                draft.reminderDaysBefore = null;
                if (current.recurrence() != null) {
                    errors.add(new ValidationException.FieldError("dueDate", "recurrence_requires_due_date"));
                }
            } else if (current.dueDate() == null && patch.reminderDaysBefore() == null) {
                draft.reminderDaysBefore = Task.DEFAULT_REMINDER_DAYS;
            }
        }
        if (patch.reminderDaysBefore() != null) {
            draft.reminderDaysBefore = patch.reminderDaysBefore().value();
            if (draft.reminderDaysBefore != null && draft.dueDate == null) {
                errors.add(new ValidationException.FieldError("reminderDaysBefore", "reminder_requires_due_date"));
            }
        }
        if (patch.tagIds() != null) {
            draft.tagIds = patch.tagIds().value();
        }
        if (patch.links() != null) {
            draft.links = patch.links().value();
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return draft.build(clock.instant());
    }

    private static void validate(CreateTaskCommand command) {
        List<ValidationException.FieldError> errors = new ArrayList<>();
        if (command.id() != null && !Ids.isV7(command.id())) {
            errors.add(new ValidationException.FieldError("id", "uuid_v7_required"));
        }
        if (command.title().isBlank()) {
            errors.add(new ValidationException.FieldError("title", "required"));
        }
        if (command.dueDate() == null && command.reminderDaysBefore() != null) {
            errors.add(new ValidationException.FieldError("reminderDaysBefore", "reminder_requires_due_date"));
        }
        if (command.dueDate() == null && command.recurrence() != null) {
            errors.add(new ValidationException.FieldError("recurrence", "recurrence_requires_due_date"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void requireKnownTags(Collection<UUID> tagIds) {
        if (!tags.existing(tagIds).containsAll(new HashSet<>(tagIds))) {
            throw ValidationException.of("tagIds", "unknown_tag");
        }
    }

    /** The position the actor acted as: set when the permission came only from holding an owner position. */
    private UUID positionActedAs(Actor actor, Task task) {
        if (actor.personId().equals(task.createdBy()) || task.ownerIds(Owner.Type.PERSON).contains(actor.personId())) {
            return null;
        }
        Set<UUID> owned = task.ownerIds(Owner.Type.POSITION);
        return holders.positionsHeldBy(actor.personId()).stream().filter(owned::contains).sorted()
                .findFirst().orElse(null);
    }

    private HistoryEntry entry(Task task, Actor actor, UUID onBehalfOf, HistoryKind kind, Changes changes) {
        return new HistoryEntry(Ids.newId(), task.groupId(), task.id(), task.updatedAt(), actor.personId(),
                onBehalfOf, kind, changes.asMap());
    }

    private static List<Owner> minus(Collection<Owner> from, Collection<Owner> removed) {
        return from.stream().filter(o -> !removed.contains(o)).toList();
    }
}
