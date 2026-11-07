package app.mikraal.bff.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/** The whole BFF in the dev profile, with MockMvc in front and a mocked activities service behind. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "spring.http.serviceclient.activities.base-url=" + WebTest.SERVICE)
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("dev")
public @interface WebTest {

    String SERVICE = "http://activities.test";
}
