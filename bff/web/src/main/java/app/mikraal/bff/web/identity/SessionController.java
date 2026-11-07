package app.mikraal.bff.web.identity;

import java.util.UUID;

import app.mikraal.bff.web.api.SessionApi;
import app.mikraal.bff.web.api.model.DevLoginRequest;
import app.mikraal.bff.web.api.model.ListDevPeople200Response;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.Session;
import app.mikraal.bff.web.api.model.SessionGroupsInner;
import app.mikraal.bff.web.dev.DevPeople;
import app.mikraal.bff.web.directory.Directory;
import app.mikraal.bff.web.directory.Person;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController implements SessionApi {

    private final Identity identity;
    private final Directory directory;
    private final ObjectProvider<DevPeople> devPeople;

    SessionController(Identity identity, Directory directory, ObjectProvider<DevPeople> devPeople) {
        this.identity = identity;
        this.directory = directory;
        this.devPeople = devPeople;
    }

    @Override
    public ResponseEntity<ListDevPeople200Response> listDevPeople() {
        return ResponseEntity.ok(new ListDevPeople200Response().items(devPeople().list()));
    }

    @Override
    public ResponseEntity<Session> devLogin(String xRequestedWith, DevLoginRequest request) {
        devPeople();
        Person person = directory.person(request.getPersonId())
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND));
        identity.signIn(person.id());
        return ResponseEntity.ok(session(person));
    }

    @Override
    public ResponseEntity<Session> getSession() {
        return identity.currentPersonId()
                .flatMap(directory::person)
                .map(person -> ResponseEntity.ok(session(person)))
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.UNAUTHORIZED));
    }

    @Override
    public ResponseEntity<Void> logout(String xRequestedWith) {
        identity.signOut();
        return ResponseEntity.noContent().build();
    }

    /** The development login exists only in the dev profile; elsewhere its endpoints are not found. */
    private DevPeople devPeople() {
        DevPeople people = devPeople.getIfAvailable();
        if (people == null) {
            throw new ErrorResponseException(HttpStatus.NOT_FOUND);
        }
        return people;
    }

    private Session session(Person person) {
        UUID personId = person.id();
        return new Session()
                .person(new NamedRef(personId, person.fullName()))
                .groups(directory.groupsWhereScouter(personId).stream()
                        .map(group -> new SessionGroupsInner(group.id(), group.name(), group.timeZone()))
                        .toList());
    }
}
