package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.api.generated.model.CreateTask;
import app.mikraal.activities.tasks.api.generated.model.LinkRef;
import app.mikraal.activities.tasks.api.generated.model.OwnerRef;
import app.mikraal.activities.tasks.api.generated.model.Recurrence;
import app.mikraal.activities.tasks.api.generated.model.RecurrencePeriod;
import app.mikraal.activities.tasks.api.generated.model.TaskAction;
import app.mikraal.activities.tasks.api.generated.model.TaskHistoryEntry;
import app.mikraal.activities.tasks.api.generated.model.TaskHistoryEntryActor;
import app.mikraal.activities.tasks.api.generated.model.TaskHistoryEntryChangesValue;
import app.mikraal.activities.tasks.api.generated.model.TaskStatus;
import app.mikraal.activities.tasks.api.generated.model.TaskTag;
import app.mikraal.activities.tasks.application.CreateTaskCommand;
import app.mikraal.activities.tasks.application.TaskView;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Tag;
import app.mikraal.activities.tasks.domain.Task;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Between the generated API models and the domain. */
final class TaskMapper {

    private TaskMapper() {
    }

    static app.mikraal.activities.tasks.api.generated.model.Task toApi(TaskView view) {
        Task task = view.task();
        var api = new app.mikraal.activities.tasks.api.generated.model.Task(
                task.id(), task.groupId(), task.title(), toApi(task.status()),
                task.owners().stream().map(TaskMapper::toApi).toList(),
                task.tagIds(),
                task.links().stream().map(TaskMapper::toApi).toList(),
                view.overdue(), task.unassigned(), task.createdBy(), utc(task.createdAt()), utc(task.updatedAt()),
                task.version(),
                view.allowedActions().stream().sorted(Comparator.naturalOrder())
                        .map(a -> TaskAction.fromValue(a.name().toLowerCase(Locale.ROOT))).toList());
        api.setDescription(task.description());
        api.setPauseReason(task.pauseReason());
        api.setDueDate(task.dueDate());
        api.setReminderDaysBefore(task.reminderDaysBefore());
        if (task.recurrence() != null) {
            api.setRecurrence(new Recurrence(task.recurrence().seriesId(),
                    RecurrencePeriod.fromValue(task.recurrence().period().code()), task.recurrence().active()));
        }
        return api;
    }

    static CreateTaskCommand toCommand(CreateTask body) {
        return new CreateTaskCommand(
                body.getId(),
                body.getTitle(),
                body.getDescription(),
                owners(body.getOwners()),
                body.getDueDate(),
                body.getReminderDaysBefore(),
                body.getTagIds() == null ? List.of() : body.getTagIds(),
                links(body.getLinks()),
                body.getRecurrence() == null ? null
                        : app.mikraal.activities.tasks.domain.RecurrencePeriod.fromCode(
                                body.getRecurrence().getPeriod().getValue()));
    }

    static List<Owner> owners(List<OwnerRef> refs) {
        return refs == null ? List.of() : refs.stream()
                .map(r -> new Owner(Owner.Type.fromCode(r.getType().getValue()), r.getId())).toList();
    }

    static List<Link> links(List<LinkRef> refs) {
        return refs == null ? List.of() : refs.stream()
                .map(r -> new Link(Link.Type.fromCode(r.getType().getValue()), r.getId())).toList();
    }

    static app.mikraal.activities.tasks.domain.TaskStatus toDomain(TaskStatus status) {
        return app.mikraal.activities.tasks.domain.TaskStatus.fromCode(status.getValue());
    }

    static TaskHistoryEntry toApi(HistoryEntry entry) {
        var actor = new TaskHistoryEntryActor(entry.actorPersonId());
        actor.setOnBehalfOfPositionId(entry.onBehalfOfPositionId());
        var api = new TaskHistoryEntry(entry.id(), utc(entry.at()), actor,
                TaskHistoryEntry.KindEnum.fromValue(entry.kind().code()));
        Map<String, TaskHistoryEntryChangesValue> changes = new LinkedHashMap<>();
        entry.changes().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            var change = new TaskHistoryEntryChangesValue();
            change.setFrom(e.getValue().from());
            change.setTo(e.getValue().to());
            changes.put(e.getKey(), change);
        });
        api.setChanges(changes);
        return api;
    }

    static TaskTag toApi(Tag tag) {
        return new TaskTag(tag.id(), tag.name());
    }

    private static TaskStatus toApi(app.mikraal.activities.tasks.domain.TaskStatus status) {
        return TaskStatus.fromValue(status.code());
    }

    private static OwnerRef toApi(Owner owner) {
        return new OwnerRef(OwnerRef.TypeEnum.fromValue(owner.type().code()), owner.id());
    }

    private static LinkRef toApi(Link link) {
        return new LinkRef(LinkRef.TypeEnum.fromValue(link.type().code()), link.id());
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
