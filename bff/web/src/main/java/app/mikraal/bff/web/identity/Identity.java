package app.mikraal.bff.web.identity;

import java.util.Optional;
import java.util.UUID;

/**
 * Who is calling. The development login signs people in; the identity provider of workshop 5
 * replaces it behind this interface.
 */
public interface Identity {

    Optional<UUID> currentPersonId();

    void signIn(UUID personId);

    void signOut();
}
