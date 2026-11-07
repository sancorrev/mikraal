package app.mikraal.activities.tasks.authz;

import java.util.Set;
import java.util.UUID;

/** Who holds which position, as the authorization graph knows it. */
public interface PositionHolders {

    Set<UUID> positionsHeldBy(UUID personId);

    Set<UUID> holdersOf(UUID positionId);
}
