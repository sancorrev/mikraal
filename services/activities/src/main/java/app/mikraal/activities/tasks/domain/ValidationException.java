package app.mikraal.activities.tasks.domain;

import java.util.List;

/** The request breaks a rule; each error names the field and a stable code the web can translate. */
public class ValidationException extends RuntimeException {

    public record FieldError(String field, String code) {
    }

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Invalid request: " + errors);
        this.errors = List.copyOf(errors);
    }

    public static ValidationException of(String field, String code) {
        return new ValidationException(List.of(new FieldError(field, code)));
    }

    public List<FieldError> errors() {
        return errors;
    }
}
