package app.mikraal.bff.web.dev;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties(DevProperties.class)
class DevConfiguration {

    @Bean
    Seed seed(DevProperties properties, JsonMapper jsonMapper) {
        return jsonMapper.readValue(Path.of(properties.seedPath()).toFile(), Seed.class);
    }
}
