package app.mikraal.bff.web.http;

import app.mikraal.bff.web.identity.Identity;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

    private final Identity identity;

    WebConfiguration(Identity identity) {
        this.identity = identity;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CsrfGuard()).addPathPatterns("/api/**");
        registry.addInterceptor(new SessionGuard(identity))
                .addPathPatterns("/api/**")
                // The session endpoints answer for themselves: login, 401 on read, idempotent logout.
                .excludePathPatterns("/api/dev/**", "/api/session", "/api/session/dev-login");
    }
}
