package app.mikraal.bff.web.directory;

import java.time.LocalDate;
import java.util.UUID;

/** A council or an event. */
public record DatedEntry(UUID id, String name, LocalDate date) {
}
