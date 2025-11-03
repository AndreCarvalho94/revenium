package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReveniumApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ReveniumApplication.class);
        String profiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        boolean isApi = profiles != null && Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch(p -> p.equalsIgnoreCase("api"));
        app.setWebApplicationType(isApi ? WebApplicationType.SERVLET : WebApplicationType.NONE);
        app.run(args);
    }

}
