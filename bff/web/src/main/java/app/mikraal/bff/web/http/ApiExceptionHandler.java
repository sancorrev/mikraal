package app.mikraal.bff.web.http;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details (RFC 9457) for the BFF's own errors; the service's errors pass through unchanged. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @ExceptionHandler
    ResponseEntity<byte[]> passThrough(RestClientResponseException error) {
        HttpHeaders headers = new HttpHeaders();
        HttpHeaders upstream = error.getResponseHeaders();
        if (upstream != null && upstream.getContentType() != null) {
            headers.setContentType(upstream.getContentType());
        }
        return new ResponseEntity<>(error.getResponseBodyAsByteArray(), headers, error.getStatusCode());
    }

    @ExceptionHandler
    ProblemDetail serviceUnreachable(ResourceAccessException error) {
        log.warn("Activities service unreachable: {}", error.getMessage());
        return withType(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The activities service is not reachable. Try again later."));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return super.createResponseEntity(body instanceof ProblemDetail problem ? withType(problem) : body,
                headers, status, request);
    }

    /** The contract requires {@code type}; Spring leaves it out unless set. */
    private static ProblemDetail withType(ProblemDetail problem) {
        if (problem.getType() == null) {
            problem.setType(ABOUT_BLANK);
        }
        return problem;
    }
}
