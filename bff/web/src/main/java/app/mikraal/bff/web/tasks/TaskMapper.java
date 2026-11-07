package app.mikraal.bff.web.tasks;

import app.mikraal.bff.web.activities.model.ActivitiesLinkRef;
import app.mikraal.bff.web.activities.model.ActivitiesOwnerRef;
import app.mikraal.bff.web.activities.model.ActivitiesRecurrence;
import app.mikraal.bff.web.activities.model.ActivitiesTask;
import app.mikraal.bff.web.activities.model.ActivitiesTaskCapabilities;
import app.mikraal.bff.web.api.model.Link;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.Owner;
import app.mikraal.bff.web.api.model.RecurrencePeriod;
import app.mikraal.bff.web.api.model.Task;
import app.mikraal.bff.web.api.model.TaskAction;
import app.mikraal.bff.web.api.model.TaskCapabilities;
import app.mikraal.bff.web.api.model.TaskRecurrence;
import app.mikraal.bff.web.api.model.TaskStatus;

/** Service task → web task, with every id resolved to a name. */
final class TaskMapper {

    private TaskMapper() {
    }

    static Task toTask(ActivitiesTask task, Names names) {
        return new Task()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(TaskStatus.fromValue(task.getStatus().getValue()))
                .pauseReason(task.getPauseReason())
                .owners(Lists.map(task.getOwners(), owner -> owner(owner, names)))
                .dueDate(task.getDueDate())
                .reminderDaysBefore(task.getReminderDaysBefore())
                .tags(Lists.map(task.getTagIds(), id -> new NamedRef(id, names.tag(id))))
                .links(Lists.map(task.getLinks(), link -> link(link, names)))
                .recurrence(recurrence(task.getRecurrence()))
                .overdue(task.getOverdue())
                .unassigned(task.getUnassigned())
                .createdBy(new NamedRef(task.getCreatedBy(), names.person(task.getCreatedBy())))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .allowedActions(Lists.map(task.getAllowedActions(), action -> TaskAction.fromValue(action.getValue())));
    }

    static TaskCapabilities toCapabilities(ActivitiesTaskCapabilities capabilities) {
        return new TaskCapabilities(capabilities.getCanCreateTasks(), capabilities.getCanManageTags());
    }

    private static Owner owner(ActivitiesOwnerRef owner, Names names) {
        String type = owner.getType().getValue();
        return new Owner(Owner.TypeEnum.fromValue(type), owner.getId(), names.of(type, owner.getId()));
    }

    private static Link link(ActivitiesLinkRef link, Names names) {
        String type = link.getType().getValue();
        return new Link(Link.TypeEnum.fromValue(type), link.getId(), names.of(type, link.getId()));
    }

    private static TaskRecurrence recurrence(ActivitiesRecurrence recurrence) {
        if (recurrence == null) {
            return null;
        }
        return new TaskRecurrence(recurrence.getSeriesId(),
                RecurrencePeriod.fromValue(recurrence.getPeriod().getValue()), recurrence.getActive());
    }
}
