package br.com.acdev.revenium.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfigs {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Revenium Metrics API")
                        .version("1.0")
                        .description("Metrics API for Revenium application."));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


}