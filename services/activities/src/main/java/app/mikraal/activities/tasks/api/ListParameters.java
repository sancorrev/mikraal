package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.application.TaskListQuery;
import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.ValidationException;
import java.util.UUID;

/** Parses the {@code assignedTo} and {@code linked} filters of the task list. */
final class ListParameters {

    private ListParameters() {
    }

    static TaskListQuery.Assignee assignee(String value) {
        if (value == null) {
            return null;
        }
        try {
            return switch (value) {
                case "me" -> new TaskListQuery.Me();
                case "unassigned" -> new TaskListQuery.Unassigned();
                default -> {
                    String[] parts = value.split(":", 2);
                    UUID id = UUID.fromString(parts[1]);
                    yield switch (parts[0]) {
                        case "person" -> new TaskListQuery.Person(id);
                        case "position" -> new TaskListQuery.Position(id);
                        default -> throw new IllegalArgumentException(parts[0]);
                    };
                }
            };
        } catch (RuntimeException e) {
            throw ValidationException.of("assignedTo", "invalid");
        }
    }

    static Link linked(String value) {
        if (value == null) {
            return null;
        }
        try {
            String[] parts = value.split(":", 2);
            return new Link(Link.Type.fromCode(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw ValidationException.of("linked", "invalid");
        }
    }
}
