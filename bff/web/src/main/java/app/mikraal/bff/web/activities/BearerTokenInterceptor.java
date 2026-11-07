package app.mikraal.bff.web.activities;

import java.io.IOException;

import app.mikraal.bff.web.directory.Directory;
import app.mikraal.bff.web.identity.Identity;
import app.mikraal.bff.web.identity.ServiceTokens;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/** Sends the session person to the service as {@code Authorization: Bearer <token>}. */
@Component
class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final Identity identity;
    private final Directory directory;
    private final ServiceTokens tokens;

    BearerTokenInterceptor(Identity identity, Directory directory, ServiceTokens tokens) {
        this.identity = identity;
        this.directory = directory;
        this.tokens = tokens;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        identity.currentPersonId()
                .flatMap(directory::person)
                .map(tokens::issueFor)
                .ifPresent(token -> request.getHeaders().setBearerAuth(token));
        return execution.execute(request, body);
    }
}
