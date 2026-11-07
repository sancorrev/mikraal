package app.mikraal.activities.tasks.authz;

import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Task;
import java.util.Collection;
import java.util.UUID;

/** Keeps the authorization graph in step with tasks: group, creator and owners. */
public interface TaskRelations {

    void created(Task task);

    void ownersChanged(UUID taskId, Collection<Owner> removed, Collection<Owner> added);

    void deleted(Task task);
}
