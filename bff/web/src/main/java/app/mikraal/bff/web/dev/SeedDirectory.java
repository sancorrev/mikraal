package app.mikraal.bff.web.dev;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.mikraal.bff.web.directory.DatedEntry;
import app.mikraal.bff.web.directory.Directory;
import app.mikraal.bff.web.directory.Entry;
import app.mikraal.bff.web.directory.Group;
import app.mikraal.bff.web.directory.GroupDirectory;
import app.mikraal.bff.web.directory.Person;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** The directory of the dev profile, read from the seed. */
@Component
@Profile("dev")
class SeedDirectory implements Directory {

    private static final Comparator<Entry> BY_NAME =
            Comparator.comparing(Entry::name, Collator.getInstance(Locale.forLanguageTag("es")));

    private final Seed seed;
    private final Map<UUID, Seed.Person> people;
    private final Map<UUID, Seed.Group> groups;

    SeedDirectory(Seed seed) {
        this.seed = seed;
        this.people = seed.people().stream().collect(Collectors.toMap(Seed.Person::id, Function.identity()));
        this.groups = seed.groups().stream().collect(Collectors.toMap(Seed.Group::id, Function.identity()));
    }

    @Override
    public Optional<Person> person(UUID personId) {
        return Optional.ofNullable(people.get(personId))
                .map(person -> new Person(person.id(), person.fullName()));
    }

    @Override
    public List<Group> groupsWhereScouter(UUID personId) {
        return Optional.ofNullable(people.get(personId)).map(Seed.Person::scouterOf).orElse(List.of()).stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .map(group -> new Group(group.id(), group.name(), group.timeZone()))
                .toList();
    }

    @Override
    public Optional<GroupDirectory> group(UUID groupId) {
        if (!groups.containsKey(groupId)) {
            return Optional.empty();
        }
        return Optional.of(new GroupDirectory(
                seed.people().stream()
                        .filter(person -> person.scouterOf().contains(groupId))
                        .map(person -> new Entry(person.id(), person.fullName()))
                        .sorted(BY_NAME)
                        .toList(),
                seed.positions().stream()
                        .filter(position -> position.groupId().equals(groupId))
                        .map(position -> new Entry(position.id(), position.name()))
                        .sorted(BY_NAME)
                        .toList(),
                dated(seed.councils(), groupId),
                dated(seed.events(), groupId)));
    }

    private static List<DatedEntry> dated(List<Seed.Item> items, UUID groupId) {
        return items.stream()
                .filter(item -> item.groupId().equals(groupId))
                .map(item -> new DatedEntry(item.id(), item.title(), item.date()))
                .sorted(Comparator.comparing(DatedEntry::date))
                .toList();
    }
}
