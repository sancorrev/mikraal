package app.mikraal.activities.tasks.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** A kraal task. Immutable: changes go through {@link #draft()}. */
public record Task(
        UUID id,
        UUID groupId,
        String title,
        String description,
        TaskStatus status,
        String pauseReason,
        List<Owner> owners,
        LocalDate dueDate,
        Integer reminderDaysBefore,
        List<UUID> tagIds,
        List<Link> links,
        Recurrence recurrence,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        int version) {

    public static final int DEFAULT_REMINDER_DAYS = 2;

    private static final Comparator<Owner> OWNER_ORDER =
            Comparator.comparing(Owner::type).thenComparing(Owner::id);
    private static final Comparator<Link> LINK_ORDER = Comparator.comparing(Link::type).thenComparing(Link::id);

    /** Collections are kept distinct and in a canonical order, so versions compare by value. */
    public Task {
        owners = owners.stream().distinct().sorted(OWNER_ORDER).toList();
        tagIds = tagIds.stream().distinct().sorted().toList();
        links = links.stream().distinct().sorted(LINK_ORDER).toList();
    }

    public boolean unassigned() {
        return owners.isEmpty();
    }

    /** Open and past its deadline, where {@code today} is the date in the group's time zone. */
    public boolean overdueOn(LocalDate today) {
        return status.isOpen() && dueDate != null && dueDate.isBefore(today);
    }

    public boolean hasActiveRecurrence() {
        return recurrence != null && recurrence.active();
    }

    public Set<UUID> ownerIds(Owner.Type type) {
        return owners.stream().filter(o -> o.type() == type).map(Owner::id).collect(Collectors.toSet());
    }

    public Draft draft() {
        return new Draft(this);
    }

    /** Mutable copy used to build the next version of a task. */
    public static final class Draft {
        private final Task base;
        public String title;
        public String description;
        public TaskStatus status;
        public String pauseReason;
        public List<Owner> owners;
        public LocalDate dueDate;
        public Integer reminderDaysBefore;
        public List<UUID> tagIds;
        public List<Link> links;
        public Recurrence recurrence;

        private Draft(Task base) {
            this.base = base;
            title = base.title;
            description = base.description;
            status = base.status;
            pauseReason = base.pauseReason;
            owners = base.owners;
            dueDate = base.dueDate;
            reminderDaysBefore = base.reminderDaysBefore;
            tagIds = base.tagIds;
            links = base.links;
            recurrence = base.recurrence;
        }

        /** The next version, stamped at {@code at}. */
        public Task build(Instant at) {
            return new Task(base.id, base.groupId, title, description, status, pauseReason, owners, dueDate,
                    reminderDaysBefore, tagIds, links, recurrence, base.createdBy, base.createdAt, at,
                    base.version + 1);
        }
    }
}
