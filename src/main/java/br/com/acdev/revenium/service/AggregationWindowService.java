package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.Commons;
import br.com.acdev.revenium.components.KeyBaseBuilder;
import br.com.acdev.revenium.components.WindowCalculator;
import br.com.acdev.revenium.domain.Aggregations;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AggregationWindowService {
    private final StringRedisTemplate redis;
    private final WindowCalculator windowCalculator;

    public Optional<Aggregations> readCurrentAggregation(UUID tenantId, UUID customerId) {
        Instant now = Instant.now();
        Instant windowStart = windowCalculator.windowStart(now);
        String base = KeyBaseBuilder.execute(tenantId, customerId, windowStart);
        String summaryKey = base + ":summary";
        boolean hasSummary = redis.hasKey(summaryKey);
        if (!hasSummary) {
            return Optional.empty();
        }
        Map<Object, Object> summary = redis.opsForHash().entries(summaryKey);
        BigInteger totalCalls = Commons.toBigInteger(summary.get("totalCalls"));
        BigInteger totalTokens = Commons.toBigInteger(summary.get("totalTokens"));
        BigInteger totalInputTokens = Commons.toBigInteger(summary.get("totalInputTokens"));
        BigInteger totalOutputTokens = Commons.toBigInteger(summary.get("totalOutputTokens"));
        BigInteger totalLatencyMs = Commons.toBigInteger(summary.get("totalLatencyMs"));
        BigDecimal avgLatencyMs = BigDecimal.ZERO;
        if (totalCalls != null && totalCalls.compareTo(BigInteger.ZERO) > 0 && totalLatencyMs != null) {
            avgLatencyMs = new BigDecimal(totalLatencyMs).divide(new BigDecimal(totalCalls), 2, RoundingMode.HALF_UP);
        }
        Map<String, Aggregations.SubAggregation> byEndpoint = buildSubAggregations(redis.opsForHash().entries(base + ":byEndpoint:calls"), redis.opsForHash().entries(base + ":byEndpoint:tokens"));
        Map<String, Aggregations.SubAggregation> byModel = buildSubAggregations(redis.opsForHash().entries(base + ":byModel:calls"), redis.opsForHash().entries(base + ":byModel:tokens"));
        Aggregations aggregations = Aggregations.builder().totalCalls(Commons.nvl(totalCalls)).totalTokens(Commons.nvl(totalTokens)).totalInputTokens(Commons.nvl(totalInputTokens)).totalOutputTokens(Commons.nvl(totalOutputTokens)).avgLatencyMs(avgLatencyMs).byEndpoint(byEndpoint).byModel(byModel).build();
        return Optional.of(aggregations);
    }

    private Map<String, Aggregations.SubAggregation> buildSubAggregations(Map<Object, Object> callsMapRaw, Map<Object, Object> tokensMapRaw) {
        Map<String, Aggregations.SubAggregation> result = new HashMap<>();
        Map<String, BigInteger> callsMap = toStringBigIntMap(callsMapRaw);
        Map<String, BigInteger> tokensMap = toStringBigIntMap(tokensMapRaw);
        Set<String> keys = new HashSet<>();
        keys.addAll(callsMap.keySet());
        keys.addAll(tokensMap.keySet());
        for (String key : keys) {
            BigInteger calls = Commons.nvl(callsMap.get(key));
            BigInteger tokens = Commons.nvl(tokensMap.get(key));
            result.put(key, Aggregations.SubAggregation.builder().calls(calls).tokens(tokens).build());
        }
        return result;
    }

    private Map<String, BigInteger> toStringBigIntMap(Map<Object, Object> raw) {
        Map<String, BigInteger> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            String k = String.valueOf(e.getKey());
            BigInteger v = Commons.toBigInteger(e.getValue());
            if (v != null) out.put(k, v);
        }
        return out;
    }
}
