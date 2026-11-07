package app.mikraal.activities.tasks.domain;

import java.util.UUID;

public record Recurrence(UUID seriesId, RecurrencePeriod period, boolean active) {
}
