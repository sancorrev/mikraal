package app.mikraal.activities.tasks.authz;

import app.mikraal.activities.tasks.domain.Owner;
import java.util.UUID;

/** OpenFGA object and user identifiers. */
final class Subjects {

    private Subjects() {
    }

    static String person(UUID id) {
        return "person:" + id;
    }

    static String group(UUID id) {
        return "group:" + id;
    }

    static String task(UUID id) {
        return "task:" + id;
    }

    static String position(UUID id) {
        return "position:" + id;
    }

    /** A person owns directly; a position owns through every current holder. */
    static String owner(Owner owner) {
        return switch (owner.type()) {
            case PERSON -> person(owner.id());
            case POSITION -> position(owner.id()) + "#holder";
        };
    }

    static UUID id(String object) {
        return UUID.fromString(object.substring(object.indexOf(':') + 1));
    }
}
