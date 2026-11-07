package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A JSON merge patch of a task. A null field is left as it is; a {@link Value} holding null clears it. */
public record TaskPatch(
        Value<String> title,
        Value<String> description,
        Value<List<Owner>> owners,
        Value<LocalDate> dueDate,
        Value<Integer> reminderDaysBefore,
        Value<List<UUID>> tagIds,
        Value<List<Link>> links) {

    public record Value<T>(T value) {
    }
}
