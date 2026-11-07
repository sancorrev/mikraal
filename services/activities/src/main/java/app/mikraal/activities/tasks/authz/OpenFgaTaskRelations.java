package app.mikraal.activities.tasks.authz;

import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.Task;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OpenFgaTaskRelations implements TaskRelations {

    private final OpenFga fga;

    OpenFgaTaskRelations(OpenFga fga) {
        this.fga = fga;
    }

    @Override
    public void created(Task task) {
        fga.write(tuples(task), List.of());
    }

    @Override
    public void ownersChanged(UUID taskId, Collection<Owner> removed, Collection<Owner> added) {
        fga.write(ownerTuples(taskId, added), ownerTuples(taskId, removed));
    }

    @Override
    public void deleted(Task task) {
        fga.write(List.of(), tuples(task));
    }

    private static List<OpenFga.Tuple> tuples(Task task) {
        String object = Subjects.task(task.id());
        List<OpenFga.Tuple> tuples = new ArrayList<>();
        tuples.add(new OpenFga.Tuple(Subjects.group(task.groupId()), "group", object));
        tuples.add(new OpenFga.Tuple(Subjects.person(task.createdBy()), "creator", object));
        tuples.addAll(ownerTuples(task.id(), task.owners()));
        return tuples;
    }

    private static List<OpenFga.Tuple> ownerTuples(UUID taskId, Collection<Owner> owners) {
        return owners.stream().map(o -> new OpenFga.Tuple(Subjects.owner(o), "owner", Subjects.task(taskId))).toList();
    }
}
