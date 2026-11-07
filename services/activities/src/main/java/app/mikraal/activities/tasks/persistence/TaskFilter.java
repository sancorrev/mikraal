package app.mikraal.activities.tasks.persistence;

import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Filters of the task list. Null fields do not filter. {@code today} is the group's date, used by
 * {@code overdue}.
 */
public record TaskFilter(
        Set<TaskStatus> statuses,
        OwnerFilter owner,
        UUID tagId,
        Link linked,
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue,
        LocalDate today) {

    public sealed interface OwnerFilter {
    }

    /** Tasks owned by any of these owners. */
    public record AnyOwner(Set<Owner> owners) implements OwnerFilter {
    }

    public record Unassigned() implements OwnerFilter {
    }
}
