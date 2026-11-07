package app.mikraal.bff.web.activities;

import app.mikraal.bff.web.activities.api.CapabilitiesClient;
import app.mikraal.bff.web.activities.api.TagsClient;
import app.mikraal.bff.web.activities.api.TasksClient;
import app.mikraal.bff.web.activities.model.ActivitiesTaskStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Clients of the activities service, generated from {@code tasks.yaml}. The base URL comes from
 * {@code spring.http.serviceclient.activities.base-url}.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = ActivitiesClientConfiguration.GROUP,
        types = {TasksClient.class, TagsClient.class, CapabilitiesClient.class})
class ActivitiesClientConfiguration {

    static final String GROUP = "activities";

    @Bean
    RestClientHttpServiceGroupConfigurer activitiesGroup(BearerTokenInterceptor bearerToken) {
        return groups -> groups.filterByName(GROUP).forEachGroup((group, client, proxyFactory) -> {
            client.requestInterceptor(bearerToken);
            proxyFactory.conversionService(queryParameterConversion());
        });
    }

    /** Query parameters carry the contract value of an enum ({@code in_progress}), not its Java name. */
    private static DefaultFormattingConversionService queryParameterConversion() {
        DefaultFormattingConversionService conversion = new DefaultFormattingConversionService();
        conversion.addConverter(ActivitiesTaskStatus.class, String.class, ActivitiesTaskStatus::getValue);
        return conversion;
    }
}
