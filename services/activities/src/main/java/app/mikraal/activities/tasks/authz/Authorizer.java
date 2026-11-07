package app.mikraal.activities.tasks.authz;

import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskAction;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The single decision point of the tasks module (ARQ-01, 4; ARQ-04). Denials throw {@code ForbiddenException}. */
public interface Authorizer {

    void require(Actor actor, UUID groupId, GroupAction action);

    Set<GroupAction> allowed(Actor actor, UUID groupId, Set<GroupAction> actions);

    void require(Actor actor, Task task, TaskAction action);

    /** The actions the caller may perform on each task, keyed by task id. */
    Map<UUID, Set<TaskAction>> allowedActions(Actor actor, Collection<Task> tasks);
}
