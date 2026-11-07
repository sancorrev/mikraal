package app.mikraal.activities.tasks.dev;

import app.mikraal.activities.tasks.application.TagCommands;
import app.mikraal.activities.tasks.authz.AuthorizationSetup;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development only: loads {@code authz/model.json}, the seed tuples and the seed tag catalogue. Idempotent, so it
 * runs on every start. The OpenFGA store itself is created by the client configuration.
 */
@Component
@Profile("dev")
class DevBootstrap implements ApplicationRunner {

    private final AuthorizationSetup authorization;
    private final TagCommands tags;
    private final SeedData seed;
    private final Path modelPath;

    DevBootstrap(AuthorizationSetup authorization, TagCommands tags, SeedData seed,
                 @Value("${mikraal.dev-bootstrap.model-path}") String modelPath) {
        this.authorization = authorization;
        this.tags = tags;
        this.seed = seed;
        this.modelPath = Path.of(modelPath);
    }

    @Override
    public void run(ApplicationArguments args) {
        authorization.loadModel(modelPath);
        authorization.writeMissing(seed.openfgaTuples().stream()
                .map(t -> new AuthorizationSetup.Relationship(t.user(), t.relation(), t.object())).toList());
        seed.groups().forEach(group -> tags.ensure(group.id(), seed.taskTags()));
    }
}
