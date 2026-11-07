package app.mikraal.bff.web.tasks;

import static app.mikraal.bff.web.SeedIds.COORDINATION;
import static app.mikraal.bff.web.SeedIds.COUNCIL;
import static app.mikraal.bff.web.SeedIds.EVENT;
import static app.mikraal.bff.web.SeedIds.GROUP;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static app.mikraal.bff.web.SeedIds.MARTA;
import static app.mikraal.bff.web.tasks.NamesFixture.TAG_CAMP;
import static app.mikraal.bff.web.tasks.NamesFixture.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import app.mikraal.bff.web.activities.model.ActivitiesLinkRef;
import app.mikraal.bff.web.activities.model.ActivitiesOwnerRef;
import app.mikraal.bff.web.activities.model.ActivitiesRecurrence;
import app.mikraal.bff.web.activities.model.ActivitiesRecurrencePeriod;
import app.mikraal.bff.web.activities.model.ActivitiesTask;
import app.mikraal.bff.web.activities.model.ActivitiesTaskAction;
import app.mikraal.bff.web.activities.model.ActivitiesTaskStatus;
import app.mikraal.bff.web.api.model.Link;
import app.mikraal.bff.web.api.model.NamedRef;
import app.mikraal.bff.web.api.model.Owner;
import app.mikraal.bff.web.api.model.RecurrencePeriod;
import app.mikraal.bff.web.api.model.Task;
import app.mikraal.bff.web.api.model.TaskAction;
import app.mikraal.bff.web.api.model.TaskStatus;
import org.junit.jupiter.api.Test;

class TaskMapperTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-07T10:00:00Z");

    @Test
    void resolvesOwnersTagsLinksAndCreator() {
        Task task = TaskMapper.toTask(serviceTask()
                .owners(List.of(owner(ActivitiesOwnerRef.TypeEnum.PERSON, MARTA),
                        owner(ActivitiesOwnerRef.TypeEnum.POSITION, COORDINATION)))
                .tagIds(List.of(TAG_CAMP))
                .links(List.of(link(ActivitiesLinkRef.TypeEnum.COUNCIL, COUNCIL),
                        link(ActivitiesLinkRef.TypeEnum.EVENT, EVENT),
                        link(ActivitiesLinkRef.TypeEnum.POSITION, COORDINATION))), NamesFixture.names());

        assertThat(task.getOwners()).containsExactly(
                new Owner(Owner.TypeEnum.PERSON, MARTA, "Marta Gómez Vidal"),
                new Owner(Owner.TypeEnum.POSITION, COORDINATION, "Coordinación de grupo"));
        assertThat(task.getTags()).containsExactly(new NamedRef(TAG_CAMP, "Campamento"));
        assertThat(task.getLinks()).containsExactly(
                new Link(Link.TypeEnum.COUNCIL, COUNCIL, "Consejo de grupo de noviembre"),
                new Link(Link.TypeEnum.EVENT, EVENT, "Salida de manada a la sierra"),
                new Link(Link.TypeEnum.POSITION, COORDINATION, "Coordinación de grupo"));
        assertThat(task.getCreatedBy()).isEqualTo(new NamedRef(LUCIA, "Lucía Martín Ortega"));
    }

    @Test
    void unknownIdsGetANeutralLabel() {
        Task task = TaskMapper.toTask(serviceTask()
                .createdBy(UNKNOWN)
                .owners(List.of(owner(ActivitiesOwnerRef.TypeEnum.PERSON, UNKNOWN),
                        owner(ActivitiesOwnerRef.TypeEnum.POSITION, UNKNOWN)))
                .tagIds(List.of(UNKNOWN))
                .links(List.of(link(ActivitiesLinkRef.TypeEnum.EVENT, UNKNOWN))), NamesFixture.names());

        assertThat(task.getCreatedBy().getName()).isEqualTo("Desconocido");
        assertThat(task.getOwners()).extracting(Owner::getName).containsOnly("Desconocido");
        assertThat(task.getTags()).extracting(NamedRef::getName).containsOnly("Desconocido");
        assertThat(task.getLinks()).extracting(Link::getName).containsOnly("Desconocido");
    }

    @Test
    void copiesTheRestOfTheTask() {
        UUID series = UUID.randomUUID();
        Task task = TaskMapper.toTask(serviceTask()
                .status(ActivitiesTaskStatus.PAUSED)
                .pauseReason("esperando presupuesto")
                .dueDate(LocalDate.parse("2026-11-20"))
                .reminderDaysBefore(2)
                .recurrence(new ActivitiesRecurrence(series, ActivitiesRecurrencePeriod.MONTHLY, true))
                .overdue(true)
                .allowedActions(List.of(ActivitiesTaskAction.CHANGE_STATUS, ActivitiesTaskAction.END_RECURRENCE)),
                NamesFixture.names());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PAUSED);
        assertThat(task.getPauseReason()).isEqualTo("esperando presupuesto");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.parse("2026-11-20"));
        assertThat(task.getReminderDaysBefore()).isEqualTo(2);
        assertThat(task.getRecurrence().getSeriesId()).isEqualTo(series);
        assertThat(task.getRecurrence().getPeriod()).isEqualTo(RecurrencePeriod.MONTHLY);
        assertThat(task.getOverdue()).isTrue();
        assertThat(task.getVersion()).isEqualTo(3);
        assertThat(task.getAllowedActions()).containsExactly(TaskAction.CHANGE_STATUS, TaskAction.END_RECURRENCE);
    }

    private static ActivitiesTask serviceTask() {
        return new ActivitiesTask()
                .id(UUID.randomUUID())
                .groupId(GROUP)
                .title("Renovar el seguro")
                .status(ActivitiesTaskStatus.PENDING)
                .overdue(false)
                .unassigned(false)
                .createdBy(LUCIA)
                .createdAt(NOW)
                .updatedAt(NOW)
                .version(3);
    }

    private static ActivitiesOwnerRef owner(ActivitiesOwnerRef.TypeEnum type, UUID id) {
        return new ActivitiesOwnerRef(type, id);
    }

    private static ActivitiesLinkRef link(ActivitiesLinkRef.TypeEnum type, UUID id) {
        return new ActivitiesLinkRef(type, id);
    }
}
