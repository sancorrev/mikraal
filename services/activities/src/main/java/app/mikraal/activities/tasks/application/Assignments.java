package app.mikraal.activities.tasks.application;

import app.mikraal.activities.tasks.authz.PositionHolders;
import app.mikraal.activities.tasks.domain.Owner;
import app.mikraal.activities.tasks.notification.NotificationPort;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Who an owner list reaches: people directly, and the current holders of each position. */
@Component
class Assignments {

    private final PositionHolders holders;
    private final NotificationPort notifications;

    Assignments(PositionHolders holders, NotificationPort notifications) {
        this.holders = holders;
        this.notifications = notifications;
    }

    Set<UUID> people(Collection<Owner> owners) {
        Set<UUID> people = new HashSet<>();
        for (Owner owner : owners) {
            switch (owner.type()) {
                case PERSON -> people.add(owner.id());
                case POSITION -> people.addAll(holders.holdersOf(owner.id()));
            }
        }
        return people;
    }

    void notifyNewOwners(UUID groupId, UUID taskId, Collection<Owner> added) {
        Set<UUID> recipients = people(added);
        if (!recipients.isEmpty()) {
            notifications.taskAssigned(groupId, taskId, recipients);
        }
    }
}
