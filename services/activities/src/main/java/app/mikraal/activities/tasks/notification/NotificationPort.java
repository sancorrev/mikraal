package app.mikraal.activities.tasks.notification;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Push and email notices about tasks (ARQ-01 group 2: notifications behind a channel port). */
public interface NotificationPort {

    /** The people who just became owners, directly or through a position. */
    void taskAssigned(UUID groupId, UUID taskId, Set<UUID> recipients);

    /** The reminder before the deadline, sent once per task and deadline. */
    void deadlineApproaching(UUID groupId, UUID taskId, LocalDate dueDate, Set<UUID> recipients);
}
