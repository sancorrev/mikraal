package app.mikraal.activities.tasks.calendar;

import app.mikraal.activities.tasks.domain.RecurrencePeriod;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Calendar-based deadlines of a recurring series. */
@Component
public class RecurrenceSchedule {

    private final RoundCalendar rounds;

    RecurrenceSchedule(RoundCalendar rounds) {
        this.rounds = rounds;
    }

    /**
     * The deadline after {@code last}. Monthly counts from the first deadline ({@code anchor}) so a series on the
     * 31st returns to the 31st after a short month. Every round keeps the distance from the start of the round.
     */
    public LocalDate next(UUID groupId, RecurrencePeriod period, LocalDate anchor, LocalDate last) {
        return switch (period) {
            case WEEKLY -> last.plusWeeks(1);
            case MONTHLY -> anchor.plusMonths(
                    ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), last.withDayOfMonth(1)) + 1);
            case ROUND -> rounds.nextRoundStart(groupId, last)
                    .plusDays(ChronoUnit.DAYS.between(rounds.roundStart(groupId, last), last));
        };
    }
}
