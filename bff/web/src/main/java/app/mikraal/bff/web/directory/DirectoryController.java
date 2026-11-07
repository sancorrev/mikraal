package app.mikraal.bff.web.directory;

import java.util.UUID;

import app.mikraal.bff.web.api.DirectoryApi;
import app.mikraal.bff.web.api.model.DatedRef;
import app.mikraal.bff.web.api.model.Directory;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.identity.Identity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DirectoryController implements DirectoryApi {

    private final app.mikraal.bff.web.directory.Directory directory;
    private final Identity identity;

    DirectoryController(app.mikraal.bff.web.directory.Directory directory, Identity identity) {
        this.directory = directory;
        this.identity = identity;
    }

    @Override
    public ResponseEntity<Directory> getDirectory(UUID groupId) {
        UUID personId = identity.currentPersonId()
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.UNAUTHORIZED));
        GroupDirectory group = directory.group(groupId)
                .filter(found -> directory.isScouter(personId, groupId))
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.FORBIDDEN));
        return ResponseEntity.ok(new Directory()
                .people(group.scouters().stream().map(DirectoryController::named).toList())
                .positions(group.positions().stream().map(DirectoryController::named).toList())
                .councils(group.councils().stream().map(DirectoryController::dated).toList())
                .events(group.events().stream().map(DirectoryController::dated).toList()));
    }

    private static NamedRef named(Entry entry) {
        return new NamedRef(entry.id(), entry.name());
    }

    private static DatedRef dated(DatedEntry entry) {
        return new DatedRef(entry.id(), entry.name(), entry.date());
    }
}
