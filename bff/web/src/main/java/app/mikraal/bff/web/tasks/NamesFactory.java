package app.mikraal.bff.web.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import app.mikraal.bff.web.activities.api.TagsClient;
import app.mikraal.bff.web.activities.model.ActivitiesTask;
import app.mikraal.bff.web.activities.model.ActivitiesTaskHistoryEntry;
import app.mikraal.bff.web.activities.model.ActivitiesTaskTag;
import app.mikraal.bff.web.directory.DatedEntry;
import app.mikraal.bff.web.directory.Directory;
import app.mikraal.bff.web.directory.Entry;
import app.mikraal.bff.web.directory.GroupDirectory;
import app.mikraal.bff.web.directory.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Builds the {@link Names} of a group from the directory and, only when the payload carries tag
 * ids, from the service's tag catalogue (fetched per request, so renames show at once).
 */
@Component
class NamesFactory {

    private static final Logger log = LoggerFactory.getLogger(NamesFactory.class);
    private static final GroupDirectory EMPTY = new GroupDirectory(List.of(), List.of(), List.of(), List.of());

    private final Directory directory;
    private final TagsClient tagsClient;
    private final MessageSource messages;

    NamesFactory(Directory directory, TagsClient tagsClient, MessageSource messages) {
        this.directory = directory;
        this.tagsClient = tagsClient;
        this.messages = messages;
    }

    Names forTasks(UUID groupId, List<ActivitiesTask> tasks) {
        return build(groupId, tasks.stream().anyMatch(task -> !task.getTagIds().isEmpty()));
    }

    Names forHistory(UUID groupId, List<ActivitiesTaskHistoryEntry> entries) {
        return build(groupId, entries.stream().anyMatch(entry -> entry.getChanges().containsKey("tagIds")));
    }

    private Names build(UUID groupId, boolean withTags) {
        GroupDirectory group = directory.group(groupId).orElse(EMPTY);
        return new Names(
                id -> directory.person(id).map(Person::fullName),
                group.positions().stream().collect(Collectors.toMap(Entry::id, Entry::name)),
                byId(group.councils()),
                byId(group.events()),
                withTags ? tagNames(groupId) : Map.of(),
                messages.getMessage("names.unknown", null, LocaleContextHolder.getLocale()));
    }

    private Map<UUID, String> tagNames(UUID groupId) {
        try {
            return tagsClient.listTaskTags(groupId).getBody().getItems().stream()
                    .collect(Collectors.toMap(ActivitiesTaskTag::getId, ActivitiesTaskTag::getName));
        } catch (RestClientException e) {
            log.warn("Tag catalogue of group {} unavailable, tags shown as unknown: {}", groupId, e.getMessage());
            return Map.of();
        }
    }

    private static Map<UUID, String> byId(List<DatedEntry> entries) {
        return entries.stream().collect(Collectors.toMap(DatedEntry::id, DatedEntry::name));
    }
}
