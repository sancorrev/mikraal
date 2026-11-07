package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.authz.Authorizer;
import app.mikraal.activities.tasks.calendar.GroupClock;
import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adds what depends on the caller and the day: allowed actions (one batch check) and overdue. */
@Component
class TaskViews {

    private final Authorizer authorizer;
    private final GroupClock clock;

    TaskViews(Authorizer authorizer, GroupClock clock) {
        this.authorizer = authorizer;
        this.clock = clock;
    }

    TaskView of(Actor actor, Task task) {
        return of(actor, task.groupId(), List.of(task)).getFirst();
    }

    List<TaskView> of(Actor actor, UUID groupId, List<Task> tasks) {
        LocalDate today = clock.today(groupId);
        var allowed = authorizer.allowedActions(actor, tasks);
        return tasks.stream()
                .map(t -> new TaskView(t, t.overdueOn(today), allowed.get(t.id())))
                .toList();
    }
}
