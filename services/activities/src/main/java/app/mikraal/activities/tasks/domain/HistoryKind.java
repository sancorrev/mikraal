package app.mikraal.activities.tasks.domain;

import java.util.Locale;

public enum HistoryKind {
    CREATED, EDITED, STATUS_CHANGED, OWNER_REMOVED_ON_LEAVE, RECURRENCE_ENDED, INSTANCE_CREATED;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HistoryKind fromCode(String code) {
        return valueOf(code.toUpperCase(Locale.ROOT));
    }
}
