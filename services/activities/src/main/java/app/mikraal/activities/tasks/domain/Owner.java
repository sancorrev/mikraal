package app.mikraal.activities.tasks.domain;

import java.util.Locale;
import java.util.UUID;

/** A task owner: a person directly, or a position (every current holder is an owner). */
public record Owner(Type type, UUID id) {

    public enum Type {
        PERSON, POSITION;

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type fromCode(String code) {
            return valueOf(code.toUpperCase(Locale.ROOT));
        }
    }

    public static Owner person(UUID id) {
        return new Owner(Type.PERSON, id);
    }

    public static Owner position(UUID id) {
        return new Owner(Type.POSITION, id);
    }
}
