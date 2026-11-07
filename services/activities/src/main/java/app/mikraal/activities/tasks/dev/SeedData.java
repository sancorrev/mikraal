package app.mikraal.activities.tasks.dev;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

/** The parts of {@code dev/seed/seed.json} this service uses. Synthetic data only. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedData(List<Group> groups, List<String> taskTags, List<Tuple> openfgaTuples) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Group(UUID id, String timeZone) {
    }

    public record Tuple(String user, String relation, String object) {
    }
}
