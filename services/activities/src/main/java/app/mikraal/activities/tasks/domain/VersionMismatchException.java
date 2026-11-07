package app.mikraal.activities.tasks.domain;

public class VersionMismatchException extends RuntimeException {

    public VersionMismatchException() {
        super("The task changed since it was read");
    }
}
