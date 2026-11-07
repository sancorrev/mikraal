package app.mikraal.activities.tasks.domain;

import java.util.UUID;

public record Tag(UUID id, UUID groupId, String name) {
}
