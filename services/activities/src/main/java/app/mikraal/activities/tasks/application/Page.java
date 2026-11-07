package app.mikraal.activities.tasks.application;

import java.util.List;

/** {@code nextCursor} is null on the last page. */
public record Page<T>(List<T> items, String nextCursor) {
}
