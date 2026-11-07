package app.mikraal.activities.tasks.domain;

/** What a caller may do with one task. Decided by the {@code Authorizer}. */
public enum TaskAction {
    CHANGE_STATUS, EDIT, DELETE, END_RECURRENCE
}
