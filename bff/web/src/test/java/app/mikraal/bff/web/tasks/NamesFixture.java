package app.mikraal.bff.web.tasks;

import static app.mikraal.bff.web.SeedIds.COORDINATION;
import static app.mikraal.bff.web.SeedIds.COUNCIL;
import static app.mikraal.bff.web.SeedIds.EVENT;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static app.mikraal.bff.web.SeedIds.MARTA;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class NamesFixture {

    static final UUID TAG_CAMP = UUID.fromString("0193b000-0000-7000-8000-000000000001");
    static final UUID UNKNOWN = UUID.fromString("0193ffff-0000-7000-8000-000000000000");

    private NamesFixture() {
    }

    static Names names() {
        Map<UUID, String> people = Map.of(LUCIA, "Lucía Martín Ortega", MARTA, "Marta Gómez Vidal");
        return new Names(
                id -> Optional.ofNullable(people.get(id)),
                Map.of(COORDINATION, "Coordinación de grupo"),
                Map.of(COUNCIL, "Consejo de grupo de noviembre"),
                Map.of(EVENT, "Salida de manada a la sierra"),
                Map.of(TAG_CAMP, "Campamento"),
                "Desconocido");
    }
}
