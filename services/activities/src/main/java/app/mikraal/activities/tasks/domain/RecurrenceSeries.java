package app.mikraal.activities.tasks.domain;

import java.time.LocalDate;
import java.util.UUID;

/** A recurring series. {@code lastDueDate} is the deadline of the newest instance the schedule produced. */
public record RecurrenceSeries(
        UUID id,
        UUID groupId,
        RecurrencePeriod period,
        LocalDate anchorDueDate,
        LocalDate lastDueDate,
        boolean active,
        UUID createdBy) {
}
