package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.domain.VersionMismatchException;

/** A task's ETag is its version, quoted. */
final class ETags {

    private ETags() {
    }

    static String of(int version) {
        return "\"" + version + "\"";
    }

    /** The version an {@code If-Match} value names. Anything that is not a version cannot match. */
    static int version(String ifMatch) {
        String value = ifMatch.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new VersionMismatchException();
        }
    }
}
