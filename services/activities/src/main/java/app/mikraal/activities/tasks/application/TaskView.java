package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskAction;
import java.util.Set;

/** A task as one caller sees it today. */
public record TaskView(Task task, boolean overdue, Set<TaskAction> allowedActions) {
}
