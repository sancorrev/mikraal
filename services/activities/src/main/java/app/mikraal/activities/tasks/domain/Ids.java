package app.mikraal.activities.tasks.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    /** Monotonic UUIDv7, so ids sort in creation order. */
    public static UUID newId() {
        return UuidCreator.getTimeOrderedEpochPlus1();
    }

    public static boolean isV7(UUID id) {
        return id.version() == 7;
    }
}
