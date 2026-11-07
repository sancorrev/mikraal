package app.mikraal.activities.tasks.domain;

import java.util.Locale;

public enum TaskStatus {
    PENDING, IN_PROGRESS, PAUSED, FINISHED;

    public boolean canMoveTo(TaskStatus target) {
        return switch (this) {
            case PENDING -> target == IN_PROGRESS || target == PAUSED || target == FINISHED;
            case IN_PROGRESS -> target == PENDING || target == PAUSED || target == FINISHED;
            case PAUSED -> target == PENDING || target == IN_PROGRESS || target == FINISHED;
            case FINISHED -> target == IN_PROGRESS;
        };
    }

    public boolean isOpen() {
        return this != FINISHED;
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static TaskStatus fromCode(String code) {
        return valueOf(code.toUpperCase(Locale.ROOT));
    }
}
