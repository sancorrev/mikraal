package app.mikraal.activities.tasks.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Collects the fields that changed between two versions, as plain JSON-ready values. */
public final class Changes {

    private final Map<String, HistoryEntry.Change> fields = new LinkedHashMap<>();

    public Changes track(String field, Object from, Object to) {
        Object plainFrom = plain(from);
        Object plainTo = plain(to);
        if (!Objects.equals(plainFrom, plainTo)) {
            fields.put(field, new HistoryEntry.Change(plainFrom, plainTo));
        }
        return this;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public Map<String, HistoryEntry.Change> asMap() {
        return Map.copyOf(fields);
    }

    /** Every field that differs between two versions of a task. */
    public static Changes between(Task before, Task after) {
        return new Changes()
                .track("title", before.title(), after.title())
                .track("description", before.description(), after.description())
                .track("status", before.status(), after.status())
                .track("pauseReason", before.pauseReason(), after.pauseReason())
                .track("owners", before.owners(), after.owners())
                .track("dueDate", before.dueDate(), after.dueDate())
                .track("reminderDaysBefore", before.reminderDaysBefore(), after.reminderDaysBefore())
                .track("tagIds", before.tagIds(), after.tagIds())
                .track("links", before.links(), after.links());
    }

    /** The initial values of a new task; empty collections are left out. */
    public static Changes created(Task task) {
        return new Changes()
                .track("title", null, task.title())
                .track("description", null, task.description())
                .track("status", null, task.status())
                .track("owners", List.of(), task.owners())
                .track("dueDate", null, task.dueDate())
                .track("reminderDaysBefore", null, task.reminderDaysBefore())
                .track("tagIds", List.of(), task.tagIds())
                .track("links", List.of(), task.links());
    }

    private static Object plain(Object value) {
        return switch (value) {
            case null -> null;
            case UUID id -> id.toString();
            case LocalDate date -> date.toString();
            case TaskStatus status -> status.code();
            case Owner owner -> Map.of("type", owner.type().code(), "id", owner.id().toString());
            case Link link -> Map.of("type", link.type().code(), "id", link.id().toString());
            case Collection<?> items -> items.stream().map(Changes::plain).toList();
            default -> value;
        };
    }
}
