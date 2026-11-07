package app.mikraal.activities.tasks.domain;

import java.util.Locale;

public enum RecurrencePeriod {
    WEEKLY, MONTHLY, ROUND;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RecurrencePeriod fromCode(String code) {
        return valueOf(code.toUpperCase(Locale.ROOT));
    }
}
