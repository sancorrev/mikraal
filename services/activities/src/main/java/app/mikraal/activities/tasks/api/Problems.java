package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.domain.ValidationException.FieldError;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** RFC 9457 problem details with mikraal problem types. Titles are for developers; the web translates codes. */
public final class Problems {

    private static final String BASE = "https://mikraal.app/problems/";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(BASE + type));
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }

    static ProblemDetail validation(List<FieldError> errors) {
        ProblemDetail problem = of(HttpStatus.BAD_REQUEST, "validation", "The request is not valid");
        problem.setProperty("errors", errors);
        return problem;
    }
}
