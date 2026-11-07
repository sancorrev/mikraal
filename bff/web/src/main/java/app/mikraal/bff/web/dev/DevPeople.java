package app.mikraal.bff.web.dev;

import java.util.List;
import java.util.UUID;

import app.mikraal.bff.web.api.model.DevPerson;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** The synthetic people offered by the development login. */
@Component
@Profile("dev")
public class DevPeople {

    private final Seed seed;

    DevPeople(Seed seed) {
        this.seed = seed;
    }

    public List<DevPerson> list() {
        return seed.people().stream()
                .map(person -> new DevPerson(person.id(), person.fullName(), !person.scouterOf().isEmpty())
                        .positions(positionNames(person.id())))
                .toList();
    }

    private List<String> positionNames(UUID personId) {
        return seed.positions().stream()
                .filter(position -> position.holders().contains(personId))
                .map(Seed.Position::name)
                .toList();
    }
}
