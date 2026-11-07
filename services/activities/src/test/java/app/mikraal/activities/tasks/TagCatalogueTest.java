package app.mikraal.activities.tasks;

import static app.mikraal.activities.tasks.Seed.GROUP;
import static app.mikraal.activities.tasks.Seed.JAVIER;
import static app.mikraal.activities.tasks.Seed.LUCIA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

class TagCatalogueTest extends IntegrationTest {

    private static final String TAGS = "/groups/" + GROUP + "/task-tags";

    /** Names are at most 40 characters. */
    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void theSeedCatalogueIsListedByName() throws Exception {
        List<String> names = StreamSupport.stream(body(get(JAVIER, TAGS)).get("items").spliterator(), false)
                .map(t -> t.get("name").stringValue()).toList();

        assertThat(names).contains("Campamento", "Familias", "Material", "Tesorería");
        assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void onlyGroupCoordinationManagesTags() throws Exception {
        assertThat(post(JAVIER, TAGS, Map.of("name", "Seguros")).getResponse().getStatus()).isEqualTo(403);

        MvcResult created = post(LUCIA, TAGS, Map.of("name", "Seguros " + unique()));
        UUID tagId = id(body(created));

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(rename(JAVIER, tagId, Map.of("name", "x")).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(delete(JAVIER, TAGS + "/" + tagId, null).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void namesAreUniqueIgnoringCase() throws Exception {
        String name = "Salidas " + unique();
        UUID tagId = id(body(post(LUCIA, TAGS, Map.of("name", name))));
        UUID other = id(body(post(LUCIA, TAGS, Map.of("name", "Otra " + unique()))));

        MvcResult duplicate = post(LUCIA, TAGS, Map.of("name", name.toUpperCase()));
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(duplicate).get("type").stringValue()).endsWith("/duplicate-tag");
        assertThat(rename(LUCIA, other, Map.of("name", name)).getResponse().getStatus())
                .isEqualTo(409);

        JsonNode renamed = body(rename(LUCIA, tagId, Map.of("name", name.toLowerCase())));
        assertThat(renamed.get("name").stringValue()).isEqualTo(name.toLowerCase());
        assertThat(rename(LUCIA, UUID.randomUUID(), Map.of("name", "x")).getResponse().getStatus())
                .isEqualTo(404);
    }

    @Test
    void removingATagRemovesItFromItsTasksAndHistoryKeepsTheRecord() throws Exception {
        UUID removed = id(body(post(LUCIA, TAGS, Map.of("name", "Temporal " + unique()))));
        UUID kept = id(body(post(LUCIA, TAGS, Map.of("name", "Fija " + unique()))));
        JsonNode task = createTask(JAVIER, Map.of("title", "Etiquetada", "tagIds", List.of(removed, kept)));

        assertThat(delete(LUCIA, TAGS + "/" + removed, null).getResponse().getStatus()).isEqualTo(204);

        JsonNode after = body(get(JAVIER, task(id(task))));
        assertThat(after.get("tagIds").toString()).isEqualTo("[\"" + kept + "\"]");
        assertThat(after.get("version").intValue()).isEqualTo(2);
        JsonNode entry = body(get(JAVIER, task(id(task)) + "/history")).get("items").get(0);
        assertThat(entry.get("kind").stringValue()).isEqualTo("edited");
        assertThat(entry.get("actor").get("personId").stringValue()).isEqualTo(LUCIA.toString());
        assertThat(entry.get("changes").get("tagIds").get("from").toString()).contains(removed.toString());
        assertThat(delete(LUCIA, TAGS + "/" + removed, null).getResponse().getStatus()).isEqualTo(404);
        assertThat(post(JAVIER, tasks(), Map.of("title", "x", "tagIds", List.of(removed))).getResponse().getStatus())
                .isEqualTo(400);
    }

    /** Renaming takes plain JSON, not a merge patch. */
    private MvcResult rename(UUID person, UUID tagId, Map<String, String> body) throws Exception {
        return call(person, MockMvcRequestBuilders.patch(TAGS + "/" + tagId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }
}
