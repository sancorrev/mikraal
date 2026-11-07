package app.mikraal.activities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ActivitiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitiesApplication.class, args);
    }
}
