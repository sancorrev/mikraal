package app.mikraal.activities.tasks.authz;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import dev.openfga.sdk.api.configuration.ClientListStoresOptions;
import dev.openfga.sdk.api.model.CreateStoreRequest;
import dev.openfga.sdk.api.model.ListStoresResponse;
import dev.openfga.sdk.api.model.Store;
import dev.openfga.sdk.errors.FgaInvalidParameterException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenFgaProperties.class)
class OpenFgaConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenFgaConfig.class);

    /** A client bound to the store named in the properties, created first when allowed. */
    @Bean
    OpenFgaClient openFgaClient(OpenFgaProperties properties) throws FgaInvalidParameterException {
        var client = new OpenFgaClient(new ClientConfiguration().apiUrl(properties.apiUrl()));
        String storeId = findStore(client, properties.storeName())
                .orElseGet(() -> createStore(client, properties));
        client.setStoreId(storeId);
        return client;
    }

    private static Optional<String> findStore(OpenFgaClient client, String name) {
        String token = null;
        do {
            var options = new ClientListStoresOptions().continuationToken(token);
            ListStoresResponse page = OpenFga.await(() -> client.listStores(options));
            Optional<String> found = page.getStores().stream()
                    .filter(s -> name.equals(s.getName())).map(Store::getId).findFirst();
            if (found.isPresent()) {
                return found;
            }
            token = page.getContinuationToken();
        } while (token != null && !token.isEmpty());
        return Optional.empty();
    }

    private static String createStore(OpenFgaClient client, OpenFgaProperties properties) {
        if (!properties.createStoreIfMissing()) {
            throw new IllegalStateException("OpenFGA store not found: " + properties.storeName());
        }
        String id = OpenFga.await(() -> client.createStore(new CreateStoreRequest().name(properties.storeName())))
                .getId();
        log.info("Created OpenFGA store {}", id);
        return id;
    }
}
