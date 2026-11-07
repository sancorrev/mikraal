package app.mikraal.activities.tasks.calendar;

import app.mikraal.activities.tasks.dev.SeedData;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Development directory: the groups of {@code dev/seed/seed.json}. Unknown groups use the default zone. */
@Component
class SeedGroupDirectory implements GroupDirectory {

    private final Map<UUID, ZoneId> zones;
    private final ZoneId defaultZone;

    SeedGroupDirectory(SeedData seed, @Value("${mikraal.tasks.default-time-zone}") ZoneId defaultZone) {
        this.zones = seed.groups().stream()
                .collect(Collectors.toUnmodifiableMap(SeedData.Group::id, g -> ZoneId.of(g.timeZone())));
        this.defaultZone = defaultZone;
    }

    @Override
    public List<UUID> groupIds() {
        return List.copyOf(zones.keySet());
    }

    @Override
    public ZoneId timeZone(UUID groupId) {
        return zones.getOrDefault(groupId, defaultZone);
    }
}
