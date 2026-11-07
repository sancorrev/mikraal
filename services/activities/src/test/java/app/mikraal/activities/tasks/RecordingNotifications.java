package app.mikraal.activities.tasks;

import app.mikraal.activities.tasks.notification.NotificationPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

class RecordingNotifications implements NotificationPort {

    record Sent(String kind, UUID taskId, LocalDate dueDate, Set<UUID> recipients) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void taskAssigned(UUID groupId, UUID taskId, Set<UUID> recipients) {
        sent.add(new Sent("assigned", taskId, null, recipients));
    }

    @Override
    public void deadlineApproaching(UUID groupId, UUID taskId, LocalDate dueDate, Set<UUID> recipients) {
        sent.add(new Sent("reminder", taskId, dueDate, recipients));
    }

    List<Sent> about(UUID taskId) {
        return sent.stream().filter(s -> s.taskId().equals(taskId)).toList();
    }
}
