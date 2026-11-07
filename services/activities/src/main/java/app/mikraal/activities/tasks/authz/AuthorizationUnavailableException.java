package app.mikraal.activities.tasks.authz;

/** OpenFGA could not answer. Never treated as a denial or an approval. */
public class AuthorizationUnavailableException extends RuntimeException {

    AuthorizationUnavailableException(Throwable cause) {
        super("Authorization service unavailable", cause);
    }
}
