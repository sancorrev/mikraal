package app.mikraal.bff.web.tasks;

import static app.mikraal.bff.web.SeedIds.COUNCIL;
import static app.mikraal.bff.web.SeedIds.GROUP;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static app.mikraal.bff.web.SeedIds.MARTA;
import static app.mikraal.bff.web.SeedIds.TREASURY;
import static app.mikraal.bff.web.WebTest.SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ConnectException;
import java.text.ParseException;
import java.util.UUID;

import app.mikraal.bff.web.Sessions;
import app.mikraal.bff.web.WebTest;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebTest
class TasksProxyTest {

    private static final UUID TASK = UUID.fromString("0193c000-0000-7000-8000-000000000001");
    private static final UUID TAG = UUID.fromString("0193b000-0000-7000-8000-000000000001");
    private static final String TASK_URL = SERVICE + "/groups/" + GROUP + "/tasks/" + TASK;
    private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

    @Autowired
    MockMvc mvc;

    @Autowired
    MockServerRestClientCustomizer restClients;

    MockRestServiceServer service;
    MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        service = restClients.getServer();
        session = Sessions.login(mvc, LUCIA);
    }

    @AfterEach
    void verifyService() {
        service.verify();
    }

    @Test
    void getTaskResolvesNamesAndKeepsTheETag() throws Exception {
        service.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(bearerTokenOf(LUCIA))
                .andRespond(withSuccess(task(3, "[\"" + TAG + "\"]"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ETAG, "\"3\""));
        expectTagCatalogue();

        mvc.perform(get("/api/groups/{groupId}/tasks/{taskId}", GROUP, TASK).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.ETAG, "\"3\""))
                .andExpect(jsonPath("$.owners[0].name").value("Marta Gómez Vidal"))
                .andExpect(jsonPath("$.owners[1].type").value("position"))
                .andExpect(jsonPath("$.owners[1].name").value("Tesorería"))
                .andExpect(jsonPath("$.tags[0].id").value(TAG.toString()))
                .andExpect(jsonPath("$.tags[0].name").value("Campamento"))
                .andExpect(jsonPath("$.links[0].name").value("Consejo de grupo de noviembre"))
                .andExpect(jsonPath("$.createdBy.name").value("Lucía Martín Ortega"))
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.groupId").doesNotExist())
                .andExpect(jsonPath("$.tagIds").doesNotExist());
    }

    @Test
    void listTasksForwardsTheFilters() throws Exception {
        service.expect(requestTo(startsWith(SERVICE + "/groups/" + GROUP + "/tasks?")))
                .andExpect(queryParam("status", "in_progress", "paused"))
                .andExpect(queryParam("assignedTo", "me"))
                .andExpect(queryParam("limit", "20"))
                .andRespond(withSuccess("{\"items\":[],\"nextCursor\":\"next\"}", MediaType.APPLICATION_JSON));

        mvc.perform(get("/api/groups/{groupId}/tasks", GROUP).session(session)
                        .queryParam("status", "in_progress,paused")
                        .queryParam("assignedTo", "me")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    @Test
    void createTaskRelaysTheStatusAndSendsNoNulls() throws Exception {
        service.expect(requestTo(SERVICE + "/groups/" + GROUP + "/tasks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.title").value("Comprar tiendas"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.description").doesNotExist())
                .andExpect(MockRestRequestMatchers.jsonPath("$.dueDate").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ETAG, "\"1\"").body(task(1, "[]")));

        mvc.perform(post("/api/groups/{groupId}/tasks", GROUP).session(session)
                        .header("X-Requested-With", "mikraal-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Comprar tiendas\"}"))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.ETAG, "\"1\""));
    }

    @Test
    void updateTaskForwardsIfMatchAndTheMergePatchUntouched() throws Exception {
        String patch = "{\"description\":null,\"dueDate\":null,\"title\":\"Renovar el seguro\"}";
        service.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.IF_MATCH, "\"3\""))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(MERGE_PATCH))
                .andExpect(MockRestRequestMatchers.content().json(patch, JsonCompareMode.STRICT))
                .andRespond(withSuccess(task(4, "[]"), MediaType.APPLICATION_JSON).header(HttpHeaders.ETAG, "\"4\""));

        mvc.perform(patch("/api/groups/{groupId}/tasks/{taskId}", GROUP, TASK).session(session)
                        .header("X-Requested-With", "mikraal-web")
                        .header(HttpHeaders.IF_MATCH, "\"3\"")
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.ETAG, "\"4\""))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void deleteTaskRelaysNoContent() throws Exception {
        service.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.IF_MATCH, "\"3\""))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mvc.perform(delete("/api/groups/{groupId}/tasks/{taskId}", GROUP, TASK).session(session)
                        .header("X-Requested-With", "mikraal-web")
                        .header(HttpHeaders.IF_MATCH, "\"3\""))
                .andExpect(status().isNoContent());
    }

    @Test
    void serviceProblemsPassThroughUnchanged() throws Exception {
        String problem = "{\"type\":\"https://mikraal.app/problems/invalid-transition\","
                + "\"title\":\"Invalid transition\",\"status\":409,\"detail\":\"finished → paused\"}";
        service.expect(requestTo(TASK_URL + "/status-changes"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem));

        mvc.perform(post("/api/groups/{groupId}/tasks/{taskId}/status-changes", GROUP, TASK).session(session)
                        .header("X-Requested-With", "mikraal-web")
                        .header(HttpHeaders.IF_MATCH, "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"paused\",\"pauseReason\":\"esperando presupuesto\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(problem));
    }

    @Test
    void anUnreachableServiceAnswers503() throws Exception {
        service.expect(requestTo(startsWith(SERVICE)))
                .andRespond(withException(new ConnectException("Connection refused")));

        mvc.perform(get("/api/groups/{groupId}/tasks", GROUP).session(session))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void historyResolvesActorsAndIdsInsideChanges() throws Exception {
        service.expect(requestTo(startsWith(TASK_URL + "/history")))
                .andRespond(withSuccess("""
                        {"items":[{"id":"0193d000-0000-7000-8000-000000000001","at":"2026-11-07T10:00:00Z",
                          "actor":{"personId":"%s","onBehalfOfPositionId":"%s"},"kind":"edited",
                          "changes":{"tagIds":{"from":[],"to":["%s"]},"title":{"from":"Seguro","to":"Renovar"}}}],
                         "nextCursor":null}
                        """.formatted(MARTA, TREASURY, TAG), MediaType.APPLICATION_JSON));
        expectTagCatalogue();

        mvc.perform(get("/api/groups/{groupId}/tasks/{taskId}/history", GROUP, TASK).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actor.name").value("Marta Gómez Vidal"))
                .andExpect(jsonPath("$.items[0].onBehalfOfPosition.name").value("Tesorería"))
                .andExpect(jsonPath("$.items[0].changes.tagIds.to[0].name").value("Campamento"))
                .andExpect(jsonPath("$.items[0].changes.title.to").value("Renovar"));
    }

    @Test
    void listTaskTagsReturnsTheCatalogue() throws Exception {
        expectTagCatalogue();

        mvc.perform(get("/api/groups/{groupId}/task-tags", GROUP).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(TAG.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Campamento"));
    }

    private void expectTagCatalogue() {
        service.expect(requestTo(SERVICE + "/groups/" + GROUP + "/task-tags"))
                .andExpect(bearerTokenOf(LUCIA))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"%s\",\"name\":\"Campamento\"}]}".formatted(TAG),
                        MediaType.APPLICATION_JSON));
    }

    private static RequestMatcher bearerTokenOf(UUID personId) {
        return request -> {
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertThat(authorization).startsWith("Bearer ");
            try {
                assertThat(SignedJWT.parse(authorization.substring(7)).getJWTClaimsSet().getSubject())
                        .isEqualTo(personId.toString());
            } catch (ParseException e) {
                throw new AssertionError(e);
            }
        };
    }

    private static String task(int version, String tagIds) {
        return """
                {"id":"%s","groupId":"%s","title":"Renovar el seguro","description":null,"status":"in_progress",
                 "pauseReason":null,"owners":[{"type":"person","id":"%s"},{"type":"position","id":"%s"}],
                 "dueDate":"2026-11-20","reminderDaysBefore":2,"tagIds":%s,
                 "links":[{"type":"council","id":"%s"}],"recurrence":null,"overdue":false,"unassigned":false,
                 "createdBy":"%s","createdAt":"2026-11-07T10:00:00Z","updatedAt":"2026-11-07T10:00:00Z",
                 "version":%d,"allowedActions":["change_status","edit"]}
                """.formatted(TASK, GROUP, MARTA, TREASURY, tagIds, COUNCIL, LUCIA, version);
    }
}
