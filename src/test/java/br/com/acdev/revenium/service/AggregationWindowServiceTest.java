package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.WindowCalculator;
import br.com.acdev.revenium.domain.Aggregations;
import br.com.acdev.revenium.repository.AggregationWindowRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AggregationWindowServiceTest {

    @Test
    void getCurrentAggregations_returnsAggregations() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        WindowCalculator windowCalculator = Mockito.mock(WindowCalculator.class);

        AggregationWindowService service = new AggregationWindowService(redis, windowCalculator);

        UUID tenant = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        Instant windowStart = Instant.ofEpochSecond(1_700_000_000L);
        when(windowCalculator.windowStart(any(Instant.class))).thenReturn(windowStart);

        String base = "usage:win:" + tenant + ":" + customer + ":" + windowStart.getEpochSecond();
        String summaryKey = base + ":summary";
        String epCallsKey = base + ":byEndpoint:calls";
        String epTokensKey = base + ":byEndpoint:tokens";
        String modelCallsKey = base + ":byModel:calls";
        String modelTokensKey = base + ":byModel:tokens";

        when(redis.hasKey(summaryKey)).thenReturn(true);

        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        Map<Object, Object> summary = new HashMap<>();
        summary.put("totalCalls", "3");
        summary.put("totalTokens", "30");
        summary.put("totalInputTokens", "12");
        summary.put("totalOutputTokens", "18");
        summary.put("totalLatencyMs", "750");

        Map<Object, Object> epCalls = new HashMap<>();
        epCalls.put("/a", "2");
        epCalls.put("/b", "1");
        Map<Object, Object> epTokens = new HashMap<>();
        epTokens.put("/a", "20");
        epTokens.put("/b", "10");

        Map<Object, Object> modelCalls = new HashMap<>();
        modelCalls.put("m1", "3");
        Map<Object, Object> modelTokens = new HashMap<>();
        modelTokens.put("m1", "30");

        when(hashOps.entries(Mockito.eq(summaryKey))).thenReturn(summary);
        when(hashOps.entries(Mockito.eq(epCallsKey))).thenReturn(epCalls);
        when(hashOps.entries(Mockito.eq(epTokensKey))).thenReturn(epTokens);
        when(hashOps.entries(Mockito.eq(modelCallsKey))).thenReturn(modelCalls);
        when(hashOps.entries(Mockito.eq(modelTokensKey))).thenReturn(modelTokens);

        Optional<Aggregations> resOpt = service.getCurrentAggregations(tenant, customer);
        assertTrue(resOpt.isPresent());
        Aggregations res = resOpt.get();
        assertEquals(0, res.avgLatencyMs().compareTo(new BigDecimal("250.00")));
        assertEquals(3, res.totalCalls().intValue());
        assertEquals(30, res.totalTokens().intValue());
        assertEquals(12, res.totalInputTokens().intValue());
        assertEquals(18, res.totalOutputTokens().intValue());

        assertEquals(2, res.byEndpoint().get("/a").calls().intValue());
        assertEquals(20, res.byEndpoint().get("/a").tokens().intValue());
        assertEquals(1, res.byEndpoint().get("/b").calls().intValue());
        assertEquals(10, res.byEndpoint().get("/b").tokens().intValue());

        assertEquals(3, res.byModel().get("m1").calls().intValue());
        assertEquals(30, res.byModel().get("m1").tokens().intValue());
    }
}

