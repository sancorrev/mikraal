package app.mikraal.bff.web.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param seedPath  the synthetic data file, {@code dev/seed/seed.json}, relative to the working directory
 * @param jwtSecret the HS256 secret shared with the activities service
 */
@ConfigurationProperties("mikraal.dev")
public record DevProperties(String seedPath, String jwtSecret) {
}
