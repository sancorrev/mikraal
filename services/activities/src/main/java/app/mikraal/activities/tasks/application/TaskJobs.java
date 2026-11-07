package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.calendar.GroupDirectory;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TaskJobs {

    private static final Logger log = LoggerFactory.getLogger(TaskJobs.class);

    private final GroupDirectory groups;
    private final RecurrenceJob recurrence;
    private final ReminderJob reminders;

    TaskJobs(GroupDirectory groups, RecurrenceJob recurrence, ReminderJob reminders) {
        this.groups = groups;
        this.recurrence = recurrence;
        this.reminders = reminders;
    }

    /** Group by group, so one failing group does not stop the others. */
    @Scheduled(cron = "${mikraal.tasks.jobs-cron}")
    void run() {
        for (UUID groupId : groups.groupIds()) {
            try {
                recurrence.run(groupId);
                reminders.run(groupId);
            } catch (RuntimeException e) {
                log.error("Task jobs failed for group {}", groupId, e);
            }
        }
    }
}
