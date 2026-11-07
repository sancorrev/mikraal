package app.mikraal.bff.web.tasks;

import java.util.List;

import app.mikraal.bff.web.activities.model.ActivitiesChangeTaskStatus;
import app.mikraal.bff.web.activities.model.ActivitiesCreateTask;
import app.mikraal.bff.web.activities.model.ActivitiesCreateTaskRecurrence;
import app.mikraal.bff.web.activities.model.ActivitiesCreateTaskTag;
import app.mikraal.bff.web.activities.model.ActivitiesLinkRef;
import app.mikraal.bff.web.activities.model.ActivitiesOwnerRef;
import app.mikraal.bff.web.activities.model.ActivitiesRecurrencePeriod;
import app.mikraal.bff.web.activities.model.ActivitiesTaskStatus;
import app.mikraal.bff.web.api.model.ChangeTaskStatus;
import app.mikraal.bff.web.api.model.CreateTask;
import app.mikraal.bff.web.api.model.CreateTaskRecurrence;
import app.mikraal.bff.web.api.model.LinkRef;
import app.mikraal.bff.web.api.model.OwnerRef;
import app.mikraal.bff.web.api.model.TagName;
import app.mikraal.bff.web.api.model.TaskStatus;

/**
 * Web request → service request. The BFF does not validate: the service does, and its
 * validation problems pass through unchanged.
 */
final class TaskRequests {

    private TaskRequests() {
    }

    static ActivitiesCreateTask create(CreateTask task) {
        return new ActivitiesCreateTask()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .owners(Lists.map(task.getOwners(), TaskRequests::owner))
                .dueDate(task.getDueDate())
                .reminderDaysBefore(task.getReminderDaysBefore())
                .tagIds(task.getTagIds())
                .links(Lists.map(task.getLinks(), TaskRequests::link))
                .recurrence(recurrence(task.getRecurrence()));
    }

    static ActivitiesChangeTaskStatus changeStatus(ChangeTaskStatus change) {
        return new ActivitiesChangeTaskStatus()
                .status(status(change.getStatus()))
                .pauseReason(change.getPauseReason());
    }

    static ActivitiesCreateTaskTag tag(TagName tag) {
        return new ActivitiesCreateTaskTag().name(tag.getName());
    }

    static List<ActivitiesTaskStatus> statuses(List<TaskStatus> statuses) {
        return Lists.map(statuses, TaskRequests::status);
    }

    private static ActivitiesTaskStatus status(TaskStatus status) {
        return status == null ? null : ActivitiesTaskStatus.fromValue(status.getValue());
    }

    private static ActivitiesOwnerRef owner(OwnerRef owner) {
        return new ActivitiesOwnerRef()
                .type(owner.getType() == null ? null : ActivitiesOwnerRef.TypeEnum.fromValue(owner.getType().getValue()))
                .id(owner.getId());
    }

    private static ActivitiesLinkRef link(LinkRef link) {
        return new ActivitiesLinkRef()
                .type(link.getType() == null ? null : ActivitiesLinkRef.TypeEnum.fromValue(link.getType().getValue()))
                .id(link.getId());
    }

    private static ActivitiesCreateTaskRecurrence recurrence(CreateTaskRecurrence recurrence) {
        if (recurrence == null) {
            return null;
        }
        return new ActivitiesCreateTaskRecurrence().period(recurrence.getPeriod() == null
                ? null
                : ActivitiesRecurrencePeriod.fromValue(recurrence.getPeriod().getValue()));
    }
}
