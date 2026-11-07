package app.mikraal.activities.tasks.calendar;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Development calendar: every round starts on 1 September. */
@Component
class SeptemberRoundCalendar implements RoundCalendar {

    @Override
    public LocalDate roundStart(UUID groupId, LocalDate date) {
        LocalDate thisYear = LocalDate.of(date.getYear(), Month.SEPTEMBER, 1);
        return date.isBefore(thisYear) ? thisYear.minusYears(1) : thisYear;
    }

    @Override
    public LocalDate nextRoundStart(UUID groupId, LocalDate date) {
        return roundStart(groupId, date).plusYears(1);
    }
}
