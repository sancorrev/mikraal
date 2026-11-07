package app.mikraal.activities.tasks.application;

import static app.mikraal.activities.tasks.authz.GroupAction.MANAGE_TAGS;
import static app.mikraal.activities.tasks.authz.GroupAction.VIEW_TASKS;

import app.mikraal.activities.tasks.authz.Authorizer;
import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.Changes;
import app.mikraal.activities.tasks.domain.ConflictException;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.HistoryKind;
import app.mikraal.activities.tasks.domain.Ids;
import app.mikraal.activities.tasks.domain.NotFoundException;
import app.mikraal.activities.tasks.domain.Tag;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.ValidationException;
import app.mikraal.activities.tasks.domain.VersionMismatchException;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.TagRepository;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** The group's tag catalogue. Changes are for group coordination. */
@Service
public class TagCommands {

    private final TenantScope scope;
    private final TagRepository tags;
    private final TaskRepository tasks;
    private final HistoryRepository history;
    private final Authorizer authorizer;
    private final Clock clock;

    TagCommands(TenantScope scope, TagRepository tags, TaskRepository tasks, HistoryRepository history,
                Authorizer authorizer, Clock clock) {
        this.scope = scope;
        this.tags = tags;
        this.tasks = tasks;
        this.history = history;
        this.authorizer = authorizer;
        this.clock = clock;
    }

    public List<Tag> list(Actor actor, UUID groupId) {
        authorizer.require(actor, groupId, VIEW_TASKS);
        return scope.call(groupId, tags::all);
    }

    public Tag create(Actor actor, UUID groupId, String name) {
        authorizer.require(actor, groupId, MANAGE_TAGS);
        Tag tag = new Tag(Ids.newId(), groupId, cleanName(name));
        scope.run(groupId, () -> {
            if (tags.nameExists(tag.name())) {
                throw ConflictException.duplicateTag();
            }
            tags.insert(tag, clock.instant());
        });
        return tag;
    }

    /** Adds the names that are missing. Used to seed a catalogue. */
    public void ensure(UUID groupId, List<String> names) {
        scope.run(groupId, () -> names.stream().map(TagCommands::cleanName).filter(n -> !tags.nameExists(n))
                .forEach(n -> tags.insert(new Tag(Ids.newId(), groupId, n), clock.instant())));
    }

    public Tag rename(Actor actor, UUID groupId, UUID tagId, String name) {
        authorizer.require(actor, groupId, MANAGE_TAGS);
        String clean = cleanName(name);
        return scope.call(groupId, () -> {
            Tag current = tags.find(tagId).orElseThrow(() -> new NotFoundException("Tag"));
            if (!current.name().equalsIgnoreCase(clean) && tags.nameExists(clean)) {
                throw ConflictException.duplicateTag();
            }
            tags.rename(tagId, clean);
            return new Tag(tagId, groupId, clean);
        });
    }

    /** Removes the tag from every task that carries it, with a history entry on each. */
    public void delete(Actor actor, UUID groupId, UUID tagId) {
        authorizer.require(actor, groupId, MANAGE_TAGS);
        scope.run(groupId, () -> {
            tags.find(tagId).orElseThrow(() -> new NotFoundException("Tag"));
            Instant now = clock.instant();
            for (Task task : tasks.carryingTag(tagId)) {
                Task.Draft draft = task.draft();
                draft.tagIds = task.tagIds().stream().filter(id -> !id.equals(tagId)).toList();
                Task next = draft.build(now);
                if (!tasks.update(next)) {
                    throw new VersionMismatchException();
                }
                history.append(new HistoryEntry(Ids.newId(), groupId, task.id(), now, actor.personId(), null,
                        HistoryKind.EDITED, Changes.between(task, next).asMap()));
            }
            tasks.detachTag(tagId);
            tags.delete(tagId);
        });
    }

    private static String cleanName(String name) {
        String clean = name.strip();
        if (clean.isEmpty()) {
            throw ValidationException.of("name", "required");
        }
        return clean;
    }
}
