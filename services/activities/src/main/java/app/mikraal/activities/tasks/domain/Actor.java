package app.mikraal.activities.tasks.domain;

import java.util.UUID;

/** The person issuing a command. */
public record Actor(UUID personId) {
}
