package app.mikraal.bff.web.identity;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** Keeps the person id in the server-side session (cookie {@code MIKRAAL_SESSION}). */
@Component
class HttpSessionIdentity implements Identity {

    private static final String PERSON_ID = HttpSessionIdentity.class.getName() + ".personId";

    /** A proxy bound to the current request. */
    private final HttpServletRequest request;

    HttpSessionIdentity(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Optional<UUID> currentPersonId() {
        return Optional.ofNullable(request.getSession(false))
                .map(session -> (UUID) session.getAttribute(PERSON_ID));
    }

    @Override
    public void signIn(UUID personId) {
        signOut();
        request.getSession(true).setAttribute(PERSON_ID, personId);
    }

    @Override
    public void signOut() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
