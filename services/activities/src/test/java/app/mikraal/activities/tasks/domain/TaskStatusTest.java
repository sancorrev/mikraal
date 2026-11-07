package app.mikraal.activities.tasks.domain;

import static app.mikraal.activities.tasks.domain.TaskStatus.FINISHED;
import static app.mikraal.activities.tasks.domain.TaskStatus.IN_PROGRESS;
import static app.mikraal.activities.tasks.domain.TaskStatus.PAUSED;
import static app.mikraal.activities.tasks.domain.TaskStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TaskStatusTest {

    @Test
    void transitionsAreExactlyTheOnesOfTheContract() {
        Map<TaskStatus, Set<TaskStatus>> allowed = Map.of(
                PENDING, EnumSet.of(IN_PROGRESS, PAUSED, FINISHED),
                IN_PROGRESS, EnumSet.of(PENDING, PAUSED, FINISHED),
                PAUSED, EnumSet.of(PENDING, IN_PROGRESS, FINISHED),
                FINISHED, EnumSet.of(IN_PROGRESS));

        for (TaskStatus from : TaskStatus.values()) {
            Set<TaskStatus> reachable = Arrays.stream(TaskStatus.values()).filter(from::canMoveTo)
                    .collect(Collectors.toSet());
            assertThat(reachable).as(from.name()).isEqualTo(allowed.get(from));
        }
    }
}
