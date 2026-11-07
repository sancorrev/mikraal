package app.mikraal.activities.tasks.authz;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class OpenFgaPositionHolders implements PositionHolders {

    private final OpenFga fga;

    OpenFgaPositionHolders(OpenFga fga) {
        this.fga = fga;
    }

    @Override
    public Set<UUID> positionsHeldBy(UUID personId) {
        return fga.listObjects(Subjects.person(personId), "holder", "position").stream()
                .map(Subjects::id).collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> holdersOf(UUID positionId) {
        return fga.read(null, "holder", Subjects.position(positionId)).stream()
                .map(OpenFga.Tuple::user)
                .filter(user -> user.startsWith("person:"))
                .map(Subjects::id).collect(Collectors.toSet());
    }
}
