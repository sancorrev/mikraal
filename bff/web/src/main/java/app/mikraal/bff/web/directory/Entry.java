package app.mikraal.bff.web.directory;

import java.util.UUID;

/** A named item of a group: a scouter or a position. */
public record Entry(UUID id, String name) {
}
