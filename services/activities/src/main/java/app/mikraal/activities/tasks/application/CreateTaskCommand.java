package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.domain.Link;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.domain.RecurrencePeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code id} is optional (client-generated UUIDv7, makes the command idempotent); so is {@code recurrence}. */
public record CreateTaskCommand(
        UUID id,
        String title,
        String description,
        List<Owner> owners,
        LocalDate dueDate,
        Integer reminderDaysBefore,
        List<UUID> tagIds,
        List<Link> links,
        RecurrencePeriod recurrence) {
}
