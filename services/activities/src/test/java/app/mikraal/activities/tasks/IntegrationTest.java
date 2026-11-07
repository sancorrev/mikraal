package app.mikraal.activities.tasks;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.openfga.OpenFGAContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the service with the dev profile against PostgreSQL 17 and OpenFGA in containers. The dev bootstrap loads
 * the real model and seed. Containers and context are shared by every test class; tests isolate their data with
 * fresh ids (e.g. a random linked event) instead of cleaning up.
 */
@SpringBootTest(properties = "mikraal.tasks.jobs-cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(IntegrationTest.TestBeans.class)
abstract class IntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../../dev/postgres/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");
    static final OpenFGAContainer OPENFGA = new OpenFGAContainer("openfga/openfga:v1.8.16");

    static {
        POSTGRES.start();
        OPENFGA.start();
    }

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/activities?currentSchema=activities"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("mikraal.openfga.api-url", OPENFGA::getHttpEndpoint);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }

        @Bean
        @Primary
        RecordingNotifications recordingNotifications() {
            return new RecordingNotifications();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    MutableClock clock;

    @Autowired
    RecordingNotifications notifications;

    @Value("${mikraal.jwt.secret}")
    String secret;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    static String tasks() {
        return "/groups/" + Seed.GROUP + "/tasks";
    }

    static String task(UUID id) {
        return tasks() + "/" + id;
    }

    MvcResult call(UUID person, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(person))).andReturn();
    }

    MvcResult get(UUID person, String path) throws Exception {
        return call(person, MockMvcRequestBuilders.get(path));
    }

    MvcResult post(UUID person, String path, Object body) throws Exception {
        return call(person, MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    MvcResult patch(UUID person, String path, String ifMatch, Object body) throws Exception {
        var request = MockMvcRequestBuilders.patch(path).contentType("application/merge-patch+json").content(json.writeValueAsString(body));
        return call(person, ifMatch == null ? request : request.header(HttpHeaders.IF_MATCH, ifMatch));
    }

    MvcResult changeStatus(UUID person, UUID taskId, String ifMatch, Object body) throws Exception {
        var request = MockMvcRequestBuilders.post(task(taskId) + "/status-changes").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        return call(person, ifMatch == null ? request : request.header(HttpHeaders.IF_MATCH, ifMatch));
    }

    MvcResult delete(UUID person, String path, String ifMatch) throws Exception {
        var request = MockMvcRequestBuilders.delete(path);
        return call(person, ifMatch == null ? request : request.header(HttpHeaders.IF_MATCH, ifMatch));
    }

    /** Creates a task as {@code person} and returns the response body. */
    JsonNode createTask(UUID person, Object body) throws Exception {
        MvcResult result = post(person, tasks(), body);
        if (result.getResponse().getStatus() != 201) {
            throw new AssertionError("Create failed: " + result.getResponse().getContentAsString());
        }
        return body(result);
    }

    JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    static UUID id(JsonNode node) {
        return UUID.fromString(node.get("id").stringValue());
    }

    static String etag(JsonNode task) {
        return "\"" + task.get("version").intValue() + "\"";
    }

    String token(UUID person) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer("mikraal-dev")
                .audience("mikraal-activities")
                .subject(person.toString())
                .claim("name", "Synthetic person")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
