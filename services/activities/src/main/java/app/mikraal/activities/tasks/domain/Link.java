package app.mikraal.activities.tasks.domain;

import java.util.Locale;
import java.util.UUID;

/** What a task relates to. Linking to a position is not assigning it. */
public record Link(Type type, UUID id) {

    public enum Type {
        COUNCIL, EVENT, POSITION;

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type fromCode(String code) {
            return valueOf(code.toUpperCase(Locale.ROOT));
        }
    }
}
