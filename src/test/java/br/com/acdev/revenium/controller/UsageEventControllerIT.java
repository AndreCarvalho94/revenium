package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.ReveniumApplication;
import br.com.acdev.revenium.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ReveniumApplication.class)
@Import(TestcontainersConfiguration.class)
class UsageEventControllerIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void shouldCreateUsageEventSuccessfully() {
        String requestBody = getUsageEvent();

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/usage-events")
                .then()
                .statusCode(200);
    }

    private String getUsageEvent() {
        return """
                {
                    "eventId": "33333333-3333-3333-3333-333333333333",
                    "timestamp": "2025-01-01T10:15:30Z",
                    "tenantId": "11111111-1111-1111-1111-111111111111",
                    "customerId": "22222222-2222-2222-2222-222222222222",
                    "apiEndpoint": "/api/completion",
                    "metadata": {
                        "tokens": 1500,
                        "model": "gpt-4",
                        "latencyMs": 234,
                        "inputTokens": 500,
                        "outputTokens": 1000
                    }
                }
                """;
    }
}
