package app.mikraal.activities.tasks.authz;

import dev.openfga.sdk.api.model.WriteAuthorizationModelRequest;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Loads a model and relationship tuples into the store, idempotently. Used by the development bootstrap. */
@Component
public class AuthorizationSetup {

    /** A relationship tuple in OpenFGA notation, e.g. {@code person:1 scouter group:2}. */
    public record Relationship(String user, String relation, String object) {
    }

    private static final Logger log = LoggerFactory.getLogger(AuthorizationSetup.class);

    private final OpenFga fga;
    private final JsonMapper json;

    AuthorizationSetup(OpenFga fga, JsonMapper json) {
        this.fga = fga;
        this.json = json;
    }

    public void loadModel(Path modelJson) {
        var model = json.readValue(modelJson.toFile(), WriteAuthorizationModelRequest.class);
        if (fga.ensureModel(model)) {
            log.info("Wrote authorization model from {}", modelJson.getFileName());
        }
    }

    /** Writes the tuples that are not stored yet (OpenFGA v1.8 rejects duplicate writes). */
    public void writeMissing(List<Relationship> tuples) {
        Set<OpenFga.Tuple> stored = new HashSet<>(fga.read(null, null, null));
        List<OpenFga.Tuple> missing = tuples.stream()
                .map(t -> new OpenFga.Tuple(t.user(), t.relation(), t.object()))
                .filter(t -> !stored.contains(t))
                .distinct()
                .toList();
        for (int from = 0; from < missing.size(); from += 100) {
            fga.write(missing.subList(from, Math.min(from + 100, missing.size())), List.of());
        }
        log.info("Wrote {} missing seed tuples", missing.size());
    }
}
