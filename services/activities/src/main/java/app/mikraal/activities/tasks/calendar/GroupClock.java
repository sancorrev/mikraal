package app.mikraal.activities.tasks.calendar;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Today's date in a group's time zone: deadlines are local dates (ARQ-01, 11). */
@Component
public class GroupClock {

    private final Clock clock;
    private final GroupDirectory groups;

    GroupClock(Clock clock, GroupDirectory groups) {
        this.clock = clock;
        this.groups = groups;
    }

    public LocalDate today(UUID groupId) {
        return LocalDate.now(clock.withZone(groups.timeZone(groupId)));
    }
}
