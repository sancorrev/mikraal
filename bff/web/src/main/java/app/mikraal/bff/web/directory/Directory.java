package app.mikraal.bff.web.directory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Names of people, positions, councils and events. The dev profile reads them from the seed;
 * the people and identity services replace that implementation later.
 */
public interface Directory {

    /** Any known person, scouter or not. */
    Optional<Person> person(UUID personId);

    List<Group> groupsWhereScouter(UUID personId);

    Optional<GroupDirectory> group(UUID groupId);

    default boolean isScouter(UUID personId, UUID groupId) {
        return groupsWhereScouter(personId).stream().anyMatch(group -> group.id().equals(groupId));
    }
}
