package app.mikraal.bff.web.dev;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The parts of {@code dev/seed/seed.json} the BFF uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Seed(
        List<Group> groups,
        List<Person> people,
        List<Position> positions,
        List<Item> councils,
        List<Item> events) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Group(UUID id, String name, String timeZone) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Person(UUID id, String firstName, String lastName, List<UUID> scouterOf) {

        public String fullName() {
            return firstName + " " + lastName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Position(UUID id, UUID groupId, String name, List<UUID> holders) {
    }

    /** A council or an event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(UUID id, UUID groupId, String title, LocalDate date) {
    }
}
