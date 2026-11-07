package app.mikraal.activities.tasks.notification;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development adapter. Logs identifiers only: no names, emails or task text (ARQ-01, 12). */
@Component
class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void taskAssigned(UUID groupId, UUID taskId, Set<UUID> recipients) {
        log.info("notify task_assigned group={} task={} recipients={}", groupId, taskId, recipients);
    }

    @Override
    public void deadlineApproaching(UUID groupId, UUID taskId, LocalDate dueDate, Set<UUID> recipients) {
        log.info("notify deadline_approaching group={} task={} due={} recipients={}", groupId, taskId, dueDate,
                recipients);
    }
}
