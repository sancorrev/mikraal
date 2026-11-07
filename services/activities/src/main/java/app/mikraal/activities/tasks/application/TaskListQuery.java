package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record TaskListQuery(
        Set<TaskStatus> statuses,
        Assignee assignedTo,
        UUID tagId,
        Link linked,
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue,
        String cursor,
        int limit) {

    /** Whose tasks: a person's include the ones assigned to the positions they hold. */
    public sealed interface Assignee {
    }

    public record Me() implements Assignee {
    }

    public record Person(UUID id) implements Assignee {
    }

    public record Position(UUID id) implements Assignee {
    }

    public record Unassigned() implements Assignee {
    }
}
