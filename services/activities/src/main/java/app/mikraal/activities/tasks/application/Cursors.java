package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.domain.ValidationException;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

/** Opaque page cursors: the sort key of the last item, base64url-encoded. */
final class Cursors {

    private static final String NO_DATE = "-";

    private Cursors() {
    }

    static String encode(TaskRepository.PageKey key) {
        return encode((key.dueDate() == null ? NO_DATE : key.dueDate().toString()) + "|" + key.id());
    }

    static TaskRepository.PageKey decodeTask(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String[] parts = decode(cursor).split("\\|", 2);
            LocalDate due = NO_DATE.equals(parts[0]) ? null : LocalDate.parse(parts[0]);
            return new TaskRepository.PageKey(due, UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw ValidationException.of("cursor", "invalid");
        }
    }

    static String encode(HistoryRepository.PageKey key) {
        return encode(key.at() + "|" + key.id());
    }

    static HistoryRepository.PageKey decodeHistory(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String[] parts = decode(cursor).split("\\|", 2);
            return new HistoryRepository.PageKey(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw ValidationException.of("cursor", "invalid");
        }
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String cursor) {
        return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    }
}
