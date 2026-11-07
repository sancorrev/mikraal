package app.mikraal.activities.tasks.calendar;

import static app.mikraal.activities.tasks.domain.RecurrencePeriod.MONTHLY;
import static app.mikraal.activities.tasks.domain.RecurrencePeriod.ROUND;
import static app.mikraal.activities.tasks.domain.RecurrencePeriod.WEEKLY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecurrenceScheduleTest {

    private final UUID group = UUID.randomUUID();
    private final RecurrenceSchedule schedule = new RecurrenceSchedule(new SeptemberRoundCalendar());

    @Test
    void weeklyAddsSevenDays() {
        LocalDate due = LocalDate.of(2026, 12, 29);

        assertThat(schedule.next(group, WEEKLY, due, due)).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    void monthlyKeepsTheDayOfTheFirstDeadline() {
        LocalDate anchor = LocalDate.of(2027, 1, 31);

        LocalDate february = schedule.next(group, MONTHLY, anchor, anchor);
        LocalDate march = schedule.next(group, MONTHLY, anchor, february);

        assertThat(february).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(march).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    void everyRoundKeepsTheDistanceFromTheStartOfTheRound() {
        assertThat(schedule.next(group, ROUND, null, LocalDate.of(2026, 10, 15))).isEqualTo(LocalDate.of(2027, 10, 15));
        // 2027-02-10 is 162 days after 2026-09-01; 162 days after 2027-09-01 is 2028-02-10.
        assertThat(schedule.next(group, ROUND, null, LocalDate.of(2027, 2, 10))).isEqualTo(LocalDate.of(2028, 2, 10));
        assertThat(schedule.next(group, ROUND, null, LocalDate.of(2027, 9, 1))).isEqualTo(LocalDate.of(2028, 9, 1));
    }
}
