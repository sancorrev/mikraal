package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.domain.Actor;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The actor of the current request: the bearer token's subject. */
@Component
class CurrentActor {

    Actor get() {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new Actor(UUID.fromString(jwt.getSubject()));
    }
}
