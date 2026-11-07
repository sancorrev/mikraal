package app.mikraal.activities.tasks;

import java.util.UUID;

/** Synthetic people and positions of {@code dev/seed/seed.json}. */
final class Seed {

    static final UUID GROUP = id(1);
    static final UUID LUCIA = id(11);   // group coordinator, holds «Coordinación de grupo»
    static final UUID JAVIER = id(12);  // holds «Tesorería»
    static final UUID MARTA = id(13);   // holds «Coordinación de sección · Manada» and «Secretaría»
    static final UUID PABLO = id(14);   // holds «Material»
    static final UUID IRENE = id(15);   // holds «LOPIVI»
    static final UUID DANIEL = id(16);  // holds «Uniformidad» and «Material»
    static final UUID ANA = id(18);     // family member, not a scouter
    static final UUID SECRETARIA = id(23);
    static final UUID MATERIAL = id(27);

    private Seed() {
    }

    private static UUID id(int suffix) {
        return UUID.fromString("0193a000-0000-7000-8000-%012d".formatted(suffix));
    }
}
