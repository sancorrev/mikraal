package app.mikraal.bff.web.tasks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntry;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntryActor;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntryChangesValue;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.TaskHistoryEntry;
import app.mikraal.bff.web.api.model.TaskHistoryEntryChangesValue;

/**
 * Service history entry → web history entry. Inside {@code changes}, the values of {@code owners}
 * and {@code links} ({@code {type, id}} objects) gain a {@code name}, and the ids of {@code tagIds}
 * become {@code {id, name}}. Anything else, or any shape not recognised, passes through as is.
 */
final class HistoryMapper {

    private HistoryMapper() {
    }

    static TaskHistoryEntry toEntry(ActivitiesTaskHistoryEntry entry, Names names) {
        ActivitiesTaskHistoryEntryActor actor = entry.getActor();
        UUID positionId = actor.getOnBehalfOfPositionId();
        return new TaskHistoryEntry()
                .id(entry.getId())
                .at(entry.getAt())
                .actor(new NamedRef(actor.getPersonId(), names.person(actor.getPersonId())))
                .onBehalfOfPosition(positionId == null ? null : new NamedRef(positionId, names.position(positionId)))
                .kind(TaskHistoryEntry.KindEnum.fromValue(entry.getKind().getValue()))
                .changes(changes(entry.getChanges(), names));
    }

    private static Map<String, TaskHistoryEntryChangesValue> changes(
            Map<String, ActivitiesTaskHistoryEntryChangesValue> changes, Names names) {
        if (changes == null) {
            return null;
        }
        return changes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, change -> {
            UnaryOperator<Object> resolve = value -> resolve(change.getKey(), value, names);
            return new TaskHistoryEntryChangesValue()
                    .from(resolve.apply(change.getValue().getFrom()))
                    .to(resolve.apply(change.getValue().getTo()));
        }, (a, b) -> a, LinkedHashMap::new));
    }

    private static Object resolve(String field, Object value, Names names) {
        return switch (field) {
            case "owners", "links" -> eachItem(value, item -> withName(item, names));
            case "tagIds" -> eachItem(value, item -> tagRef(item, names));
            default -> value;
        };
    }

    private static Object eachItem(Object value, UnaryOperator<Object> resolve) {
        return value instanceof List<?> list ? list.stream().map(resolve).toList() : resolve.apply(value);
    }

    private static Object withName(Object item, Names names) {
        if (item instanceof Map<?, ?> ref && ref.get("type") instanceof String type
                && ref.get("id") instanceof String id) {
            return uuid(id).map(uuid -> {
                Map<Object, Object> named = new LinkedHashMap<>(ref);
                named.put("name", names.of(type, uuid));
                return (Object) named;
            }).orElse(item);
        }
        return item;
    }

    private static Object tagRef(Object item, Names names) {
        if (item instanceof String id) {
            return uuid(id).map(uuid -> (Object) Map.of("id", id, "name", names.tag(uuid))).orElse(item);
        }
        return item;
    }

    private static Optional<UUID> uuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
