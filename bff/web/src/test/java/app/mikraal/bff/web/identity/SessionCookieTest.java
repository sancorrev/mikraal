package app.mikraal.bff.web.identity;

import static app.mikraal.bff.web.SeedIds.LUCIA;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** The cookie attributes come from the real servlet container, so this test runs one. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class SessionCookieTest {

    @LocalServerPort
    int port;

    @Test
    void theSessionCookieIsHttpOnlyAndSameSiteLax() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/session/dev-login"))
                        .header("X-Requested-With", "mikraal-web")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"personId\":\"" + LUCIA + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie -> assertThat(cookie)
                .startsWith("MIKRAAL_SESSION=")
                .containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("SameSite=Lax"));
    }
}
