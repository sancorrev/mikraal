package app.mikraal.activities.tasks.domain;

/** The request is valid but conflicts with the current state. {@code type} is the problem slug. */
public class ConflictException extends RuntimeException {

    private final String type;

    private ConflictException(String type, String detail) {
        super(detail);
        this.type = type;
    }

    public static ConflictException invalidTransition(TaskStatus from, TaskStatus to) {
        return new ConflictException("invalid-transition",
                "A task cannot move from " + from.code() + " to " + to.code());
    }

    public static ConflictException duplicateTag() {
        return new ConflictException("duplicate-tag", "A tag with that name already exists");
    }

    public static ConflictException noActiveRecurrence() {
        return new ConflictException("no-active-recurrence", "The task has no active recurrence");
    }

    /** The client-generated id belongs to a task that was deleted or is not visible to the caller. */
    public static ConflictException idInUse() {
        return new ConflictException("id-in-use", "This id is already used");
    }

    public String type() {
        return type;
    }
}
