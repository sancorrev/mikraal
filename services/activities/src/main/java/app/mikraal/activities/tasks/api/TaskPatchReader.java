package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.application.TaskPatch;
import app.mikraal.activities.tasks.application.TaskPatch.Value;
import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.ValidationException;
import app.mikraal.activities.tasks.domain.ValidationException.FieldError;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads an {@code UpdateTask} JSON merge patch (RFC 7396) with the constraints of the contract. A raw node is needed
 * because a field set to null (clear it) and a missing field (keep it) mean different things.
 */
final class TaskPatchReader {

    private static final int MAX_ITEMS = 20;
    private static final Set<String> FIELDS =
            Set.of("title", "description", "owners", "dueDate", "reminderDaysBefore", "tagIds", "links");

    private final List<FieldError> errors = new ArrayList<>();

    private TaskPatchReader() {
    }

    static TaskPatch read(ObjectNode body) {
        return new TaskPatchReader().parse(body);
    }

    private TaskPatch parse(ObjectNode body) {
        if (body.isEmpty()) {
            throw ValidationException.of("body", "required");
        }
        for (Map.Entry<String, JsonNode> property : body.properties()) {
            if (!FIELDS.contains(property.getKey())) {
                errors.add(new FieldError(property.getKey(), "not_allowed"));
            }
        }
        var patch = new TaskPatch(
                field(body, "title", (name, node) -> text(name, node, 200, false)),
                field(body, "description", (name, node) -> text(name, node, 5000, true)),
                field(body, "owners", (name, node) -> list(name, node, this::owner)),
                field(body, "dueDate", this::date),
                field(body, "reminderDaysBefore", this::reminder),
                field(body, "tagIds", (name, node) -> list(name, node, this::uuid)),
                field(body, "links", (name, node) -> list(name, node, this::link)));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return patch;
    }

    private <T> Value<T> field(ObjectNode body, String name, BiFunction<String, JsonNode, T> reader) {
        JsonNode node = body.get(name);
        return node == null ? null : new Value<>(node.isNull() ? null : reader.apply(name, node));
    }

    private String text(String name, JsonNode node, int maxLength, boolean nullable) {
        if (!node.isString()) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
        String value = node.stringValue();
        if (!nullable && value.isBlank()) {
            errors.add(new FieldError(name, "required"));
        } else if (value.length() > maxLength) {
            errors.add(new FieldError(name, "too_long"));
        }
        return value;
    }

    private LocalDate date(String name, JsonNode node) {
        try {
            return LocalDate.parse(node.stringValue());
        } catch (RuntimeException e) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
    }

    private Integer reminder(String name, JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
        int days = node.intValue();
        if (days < 0 || days > 30) {
            errors.add(new FieldError(name, "out_of_range"));
        }
        return days;
    }

    private <T> List<T> list(String name, JsonNode node, BiFunction<String, JsonNode, T> item) {
        if (!node.isArray()) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
        if (node.size() > MAX_ITEMS) {
            errors.add(new FieldError(name, "too_many"));
        }
        List<T> items = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            items.add(item.apply(name + "[" + i + "]", node.get(i)));
        }
        return items;
    }

    private UUID uuid(String name, JsonNode node) {
        try {
            return UUID.fromString(node.stringValue());
        } catch (RuntimeException e) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
    }

    private Owner owner(String name, JsonNode node) {
        try {
            return new Owner(Owner.Type.fromCode(node.get("type").stringValue()),
                    UUID.fromString(node.get("id").stringValue()));
        } catch (RuntimeException e) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
    }

    private Link link(String name, JsonNode node) {
        try {
            return new Link(Link.Type.fromCode(node.get("type").stringValue()),
                    UUID.fromString(node.get("id").stringValue()));
        } catch (RuntimeException e) {
            errors.add(new FieldError(name, "invalid"));
            return null;
        }
    }
}
