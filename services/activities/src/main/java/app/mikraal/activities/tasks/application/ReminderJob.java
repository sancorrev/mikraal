package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.calendar.GroupClock;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.notification.NotificationPort;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Sends the reminder N days before the deadline to the owners, once per task and deadline. */
@Component
public class ReminderJob {

    private final TenantScope scope;
    private final TaskRepository tasks;
    private final Assignments assignments;
    private final NotificationPort notifications;
    private final GroupClock clock;

    ReminderJob(TenantScope scope, TaskRepository tasks, Assignments assignments, NotificationPort notifications,
                GroupClock clock) {
        this.scope = scope;
        this.tasks = tasks;
        this.assignments = assignments;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** Marks first, then notifies after commit: a failure skips a reminder rather than sending it twice. */
    public void run(UUID groupId) {
        LocalDate today = clock.today(groupId);
        List<Task> due = scope.call(groupId, () -> {
            List<Task> found = tasks.dueForReminder(today);
            found.forEach(t -> tasks.markReminderSent(t.id(), t.dueDate()));
            return found;
        });
        for (Task task : due) {
            Set<UUID> recipients = assignments.people(task.owners());
            if (!recipients.isEmpty()) {
                notifications.deadlineApproaching(groupId, task.id(), task.dueDate(), recipients);
            }
        }
    }
}
