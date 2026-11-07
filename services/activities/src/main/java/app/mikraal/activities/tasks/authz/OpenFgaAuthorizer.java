package app.mikraal.activities.tasks.authz;

import app.mikraal.activities.tasks.domain.Actor;
import app.mikraal.activities.tasks.domain.ForbiddenException;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps module actions to the relations of {@code authz/model.fga} and asks OpenFGA. */
@Component
class OpenFgaAuthorizer implements Authorizer {

    private static final List<TaskAction> CHECKED = List.of(TaskAction.CHANGE_STATUS, TaskAction.EDIT, TaskAction.DELETE);

    private final OpenFga fga;

    OpenFgaAuthorizer(OpenFga fga) {
        this.fga = fga;
    }

    @Override
    public void require(Actor actor, UUID groupId, GroupAction action) {
        if (!fga.check(groupCheck(actor, groupId, action))) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Set<GroupAction> allowed(Actor actor, UUID groupId, Set<GroupAction> actions) {
        List<GroupAction> asked = List.copyOf(actions);
        List<Boolean> answers = fga.check(asked.stream().map(a -> groupCheck(actor, groupId, a)).toList());
        Set<GroupAction> allowed = EnumSet.noneOf(GroupAction.class);
        for (int i = 0; i < asked.size(); i++) {
            if (answers.get(i)) {
                allowed.add(asked.get(i));
            }
        }
        return allowed;
    }

    @Override
    public void require(Actor actor, Task task, TaskAction action) {
        if (!fga.check(taskCheck(actor, task.id(), action))) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Map<UUID, Set<TaskAction>> allowedActions(Actor actor, Collection<Task> tasks) {
        List<OpenFga.Tuple> checks = new ArrayList<>();
        for (Task task : tasks) {
            CHECKED.forEach(action -> checks.add(taskCheck(actor, task.id(), action)));
        }
        List<Boolean> answers = fga.check(checks);
        Map<UUID, Set<TaskAction>> allowed = new HashMap<>();
        int i = 0;
        for (Task task : tasks) {
            Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
            for (TaskAction action : CHECKED) {
                if (answers.get(i++)) {
                    actions.add(action);
                }
            }
            if (actions.contains(TaskAction.EDIT) && task.hasActiveRecurrence()) {
                actions.add(TaskAction.END_RECURRENCE);
            }
            allowed.put(task.id(), actions);
        }
        return allowed;
    }

    private static OpenFga.Tuple groupCheck(Actor actor, UUID groupId, GroupAction action) {
        String relation = switch (action) {
            case VIEW_TASKS -> "scouter";
            case CREATE_TASKS -> "can_create_tasks";
            case MANAGE_TAGS -> "can_manage_tags";
        };
        return new OpenFga.Tuple(Subjects.person(actor.personId()), relation, Subjects.group(groupId));
    }

    private static OpenFga.Tuple taskCheck(Actor actor, UUID taskId, TaskAction action) {
        String relation = switch (action) {
            case CHANGE_STATUS -> "can_change_status";
            case EDIT, END_RECURRENCE -> "can_edit";
            case DELETE -> "can_delete";
        };
        return new OpenFga.Tuple(Subjects.person(actor.personId()), relation, Subjects.task(taskId));
    }
}
