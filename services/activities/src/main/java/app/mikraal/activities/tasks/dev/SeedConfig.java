package app.mikraal.activities.tasks.dev;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class SeedConfig {

    @Bean
    SeedData seedData(JsonMapper json, @Value("${mikraal.seed.path}") String path) {
        return json.readValue(Path.of(path).toFile(), SeedData.class);
    }
}
