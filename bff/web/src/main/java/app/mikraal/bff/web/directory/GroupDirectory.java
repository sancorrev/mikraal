package app.mikraal.bff.web.directory;

import java.util.List;

/** What a group's pickers show. Families never appear in {@code scouters}. */
public record GroupDirectory(
        List<Entry> scouters,
        List<Entry> positions,
        List<DatedEntry> councils,
        List<DatedEntry> events) {
}
