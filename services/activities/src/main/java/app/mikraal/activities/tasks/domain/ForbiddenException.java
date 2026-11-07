package app.mikraal.activities.tasks.domain;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Not allowed");
    }
}
