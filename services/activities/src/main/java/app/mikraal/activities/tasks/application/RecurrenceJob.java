package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.authz.TaskRelations;
import app.mikraal.activities.tasks.calendar.GroupClock;
import app.mikraal.activities.tasks.calendar.RecurrenceSchedule;
import app.mikraal.activities.tasks.domain.Changes;
import app.mikraal.activities.tasks.domain.HistoryEntry;
import app.mikraal.activities.tasks.domain.HistoryKind;
import app.mikraal.activities.tasks.domain.Ids;
import app.mikraal.activities.tasks.domain.Recurrence;
import app.mikraal.activities.tasks.domain.RecurrenceSeries;
import app.mikraal.activities.tasks.domain.Task;
import app.mikraal.activities.tasks.domain.TaskStatus;
import app.mikraal.activities.tasks.persistence.HistoryRepository;
import app.mikraal.activities.tasks.persistence.SeriesRepository;
import app.mikraal.activities.tasks.persistence.TaskRepository;
import app.mikraal.activities.tasks.persistence.TenantScope;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Calendar-based recurrence: when the newest deadline of an active series arrives, the next instance is created
 * one period later, whether or not the previous one is finished.
 */
@Component
public class RecurrenceJob {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceJob.class);

    private final TenantScope scope;
    private final SeriesRepository series;
    private final TaskRepository tasks;
    private final HistoryRepository history;
    private final TaskRelations relations;
    private final RecurrenceSchedule schedule;
    private final GroupClock groupClock;
    private final Clock clock;

    RecurrenceJob(TenantScope scope, SeriesRepository series, TaskRepository tasks, HistoryRepository history,
                  TaskRelations relations, RecurrenceSchedule schedule, GroupClock groupClock, Clock clock) {
        this.scope = scope;
        this.series = series;
        this.tasks = tasks;
        this.history = history;
        this.relations = relations;
        this.schedule = schedule;
        this.groupClock = groupClock;
        this.clock = clock;
    }

    public void run(UUID groupId) {
        LocalDate today = groupClock.today(groupId);
        List<RecurrenceSeries> due = scope.call(groupId, () -> series.due(today));
        for (RecurrenceSeries s : due) {
            scope.run(groupId, () -> catchUp(s, today));
        }
    }

    private void catchUp(RecurrenceSeries s, LocalDate today) {
        LocalDate last = s.lastDueDate();
        while (!last.isAfter(today)) {
            Optional<Task> source = tasks.latestInSeries(s.id());
            if (source.isEmpty()) {
                log.info("Series {} has no live instance to copy; skipped", s.id());
                return;
            }
            last = schedule.next(s.groupId(), s.period(), s.anchorDueDate(), last);
            Task instance = instanceOf(s, source.get(), last, clock.instant());
            tasks.insert(instance);
            history.append(new HistoryEntry(Ids.newId(), s.groupId(), instance.id(), instance.createdAt(),
                    s.createdBy(), null, HistoryKind.INSTANCE_CREATED, Changes.created(instance).asMap()));
            relations.created(instance);
            series.advance(s.id(), last);
        }
    }

    /** Copies title, description, owners, tags, links and reminder; the series creator is the creator. */
    private static Task instanceOf(RecurrenceSeries s, Task source, LocalDate dueDate, Instant now) {
        return new Task(Ids.newId(), s.groupId(), source.title(), source.description(), TaskStatus.PENDING, null,
                source.owners(), dueDate, source.reminderDaysBefore(), source.tagIds(), source.links(),
                new Recurrence(s.id(), s.period(), true), s.createdBy(), now, now, 1);
    }
}
