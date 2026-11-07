package app.mikraal.activities.tasks.api;

import app.mikraal.activities.tasks.authz.AuthorizationUnavailableException;
import app.mikraal.activities.tasks.domain.ConflictException;
import app.mikraal.activities.tasks.domain.ForbiddenException;
import app.mikraal.activities.tasks.domain.NotFoundException;
import app.mikraal.activities.tasks.domain.ValidationException;
import app.mikraal.activities.tasks.domain.ValidationException.FieldError;
import app.mikraal.activities.tasks.domain.VersionMismatchException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/** Maps errors to problem details. Validation problems carry {@code errors[].field} and a stable {@code code}. */
@RestControllerAdvice
class ProblemHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);

    @ExceptionHandler
    ResponseEntity<Object> validation(ValidationException e) {
        return respond(Problems.validation(e.errors()));
    }

    @ExceptionHandler
    ResponseEntity<Object> notFound(NotFoundException e) {
        return respond(Problems.of(HttpStatus.NOT_FOUND, "not-found", e.getMessage()));
    }

    @ExceptionHandler
    ResponseEntity<Object> forbidden(ForbiddenException e) {
        return respond(Problems.of(HttpStatus.FORBIDDEN, "forbidden", "The caller may not do this"));
    }

    @ExceptionHandler
    ResponseEntity<Object> versionMismatch(VersionMismatchException e) {
        return respond(Problems.of(HttpStatus.PRECONDITION_FAILED, "version-mismatch", e.getMessage()));
    }

    @ExceptionHandler
    ResponseEntity<Object> conflict(ConflictException e) {
        return respond(Problems.of(HttpStatus.CONFLICT, e.type(), e.getMessage()));
    }

    @ExceptionHandler
    ResponseEntity<Object> authorizationUnavailable(AuthorizationUnavailableException e) {
        log.error("Authorization service unavailable", e);
        return respond(Problems.of(HttpStatus.SERVICE_UNAVAILABLE, "authorization-unavailable", e.getMessage()));
    }

    /** Query and header constraints, checked by method validation on the generated interfaces. */
    @ExceptionHandler
    ResponseEntity<Object> constraintViolation(ConstraintViolationException e) {
        return respond(Problems.validation(e.getConstraintViolations().stream()
                .map(v -> new FieldError(lastNode(v.getPropertyPath()), code(new String[] {
                        v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()},
                        v.getInvalidValue())))
                .toList()));
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (e instanceof MissingRequestHeaderException missing
                && HttpHeaders.IF_MATCH.equalsIgnoreCase(missing.getHeaderName())) {
            return respond(Problems.of(HttpStatus.PRECONDITION_REQUIRED, "if-match-required",
                    "Send the task's ETag in If-Match"));
        }
        return super.handleServletRequestBindingException(e, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(Problems.validation(e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldError(f.getField(), code(f.getCodes(), f.getRejectedValue())))
                .toList()));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = e instanceof MethodArgumentTypeMismatchException m ? m.getName() : e.getPropertyName();
        return respond(Problems.validation(List.of(new FieldError(field, "invalid"))));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = e.getCause() instanceof JacksonException json && !json.getPath().isEmpty()
                ? json.getPath().stream()
                        .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                        .collect(Collectors.joining(".")).replace(".[", "[")
                : "body";
        return respond(Problems.validation(List.of(new FieldError(field, "invalid"))));
    }

    /** A stable code from a Bean Validation constraint. */
    private static String code(String[] codes, Object rejected) {
        List<String> all = codes == null ? List.of() : Arrays.asList(codes);
        if (all.contains("NotNull")) {
            return "required";
        }
        if (all.contains("Size")) {
            if (rejected instanceof CharSequence text) {
                return text.isEmpty() ? "required" : "too_long";
            }
            return "too_many";
        }
        if (all.contains("Min") || all.contains("Max")) {
            return "out_of_range";
        }
        return "invalid";
    }

    private static String lastNode(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }

    private static ResponseEntity<Object> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
