package app.mikraal.activities.tasks.calendar;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** The groups this service serves and their time zones. Replaced by the groups service later. */
public interface GroupDirectory {

    List<UUID> groupIds();

    ZoneId timeZone(UUID groupId);
}
