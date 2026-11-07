package app.mikraal.activities.tasks.authz;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mikraal.openfga")
public record OpenFgaProperties(String apiUrl, String storeName, boolean createStoreIfMissing) {
}
