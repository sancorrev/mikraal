package app.mikraal.activities.tasks.calendar;

import java.time.LocalDate;
import java.util.UUID;

/** Start dates of a group's rounds (scout years). */
public interface RoundCalendar {

    /** The start of the round that contains {@code date}. */
    LocalDate roundStart(UUID groupId, LocalDate date);

    /** The start of the round after the one that contains {@code date}. */
    LocalDate nextRoundStart(UUID groupId, LocalDate date);
}
