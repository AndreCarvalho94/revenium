package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.ReveniumApplication;
import br.com.acdev.revenium.config.TestcontainersConfiguration;
import br.com.acdev.revenium.domain.entity.Customer;
import br.com.acdev.revenium.domain.entity.Tenant;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.service.UsageEventService;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ReveniumApplication.class)
@Import(TestcontainersConfiguration.class)
class AggregationControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private UsageEventService usageEventService;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void shouldReadCurrentAggregation() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Instant baseTs = Instant.now();
        for (int i = 0; i < 10; i++) {
            UsageEvent event = newUsageEvent(tenantId, customerId, "/api/completion", baseTs);
            usageEventService.create(event);
        }

        given()
                .contentType("application/json")
                .queryParam("tenantId", tenantId)
                .queryParam("customerId", customerId)
                .when()
                .get("/aggregations/current")
                .then()
                .statusCode(200)
                .body("totalCalls", equalTo(10))
                .body("totalTokens", equalTo(1000))
                .body("totalInputTokens", equalTo(400))
                .body("totalOutputTokens", equalTo(600))
                .body("avgLatencyMs", notNullValue())
                .body("byEndpoint['/api/completion'].calls", equalTo(10))
                .body("byEndpoint['/api/completion'].tokens", equalTo(1000))
                .body("byModel.gpt-4.calls", equalTo(10))
                .body("byModel.gpt-4.tokens", equalTo(1000));
    }

    private UsageEvent newUsageEvent(UUID tenantId, UUID customerId, String endpoint, Instant timestamp) {
        UsageEvent event = new UsageEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(timestamp);
        event.setApiEndpoint(endpoint);
        event.setMetadata(
                """
                {
                  "tokens": 100,
                  "model": "gpt-4",
                  "latencyMs": 200,
                  "inputTokens": 40,
                  "outputTokens": 60
                }
                """
        );
        event.setTenant(em.getReference(Tenant.class, tenantId));
        event.setTenantId(tenantId);
        event.setCustomer(em.getReference(Customer.class, customerId));
        event.setCustomerId(customerId);
        return event;
    }
}
