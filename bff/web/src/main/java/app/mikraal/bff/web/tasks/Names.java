package app.mikraal.bff.web.tasks;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Names for the ids of one group. Ids it does not know get a neutral label, never an error. */
final class Names {

    private final Function<UUID, Optional<String>> people;
    private final Map<UUID, String> positions;
    private final Map<UUID, String> councils;
    private final Map<UUID, String> events;
    private final Map<UUID, String> tags;
    private final String unknown;

    Names(Function<UUID, Optional<String>> people, Map<UUID, String> positions, Map<UUID, String> councils,
            Map<UUID, String> events, Map<UUID, String> tags, String unknown) {
        this.people = people;
        this.positions = positions;
        this.councils = councils;
        this.events = events;
        this.tags = tags;
        this.unknown = unknown;
    }

    String person(UUID id) {
        return people.apply(id).orElse(unknown);
    }

    String position(UUID id) {
        return positions.getOrDefault(id, unknown);
    }

    String tag(UUID id) {
        return tags.getOrDefault(id, unknown);
    }

    /** The name of a typed reference: an owner ({@code person}, {@code position}) or a link. */
    String of(String type, UUID id) {
        return switch (type) {
            case "person" -> person(id);
            case "position" -> position(id);
            case "council" -> councils.getOrDefault(id, unknown);
            case "event" -> events.getOrDefault(id, unknown);
            default -> unknown;
        };
    }
}
