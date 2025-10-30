package br.com.acdev.revenium.components;

import br.com.acdev.revenium.config.ReveniumProperties;
import br.com.acdev.revenium.domain.Metadata;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class RedisUsageAccumulator {

    private final StringRedisTemplate redis;
    private final WindowCalculator windowCalculator;
    private final ReveniumProperties properties;

    /**
     * Conveniência: calcula o windowStart a partir do evento e delega para a sobrecarga principal.
     */
    public void accumulate(UsageEvent event, Metadata metadata) {
        Instant ws = windowCalculator.windowStart(event.getTimestamp());
        accumulate(
                event.getTenantId(),
                event.getCustomerId(),
                ws,
                event.getApiEndpoint(),
                metadata.model(),
                metadata.tokens(),
                metadata.inputTokens(),
                metadata.outputTokens(),
                metadata.latencyMs()
        );
    }

    /**
     * API simplificada: assume dados válidos.
     */
    public void accumulate(
            UUID tenantId,
            UUID customerId,
            Instant windowStart,
            String endpoint,
            String model,
            BigInteger tokens,
            BigInteger inputTokens,
            BigInteger outputTokens,
            BigInteger latencyMs
    ) {
        String base = keyBase(tenantId, customerId, windowStart);

        // Resumo
        String summaryKey = base + ":summary";
        redis.opsForHash().increment(summaryKey, "totalCalls", 1);
        redis.opsForHash().increment(summaryKey, "totalTokens", tokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalInputTokens", inputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalOutputTokens", outputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalLatencyMs", latencyMs.longValueExact());
        applyExpire(summaryKey, windowStart);

        // Por endpoint
        String epCallsKey = base + ":byEndpoint:calls";
        String epTokensKey = base + ":byEndpoint:tokens";
        redis.opsForHash().increment(epCallsKey, endpoint, 1);
        redis.opsForHash().increment(epTokensKey, endpoint, tokens.longValueExact());
        applyExpire(epCallsKey, windowStart);
        applyExpire(epTokensKey, windowStart);

        // Por modelo
        String mCallsKey = base + ":byModel:calls";
        String mTokensKey = base + ":byModel:tokens";
        redis.opsForHash().increment(mCallsKey, model, 1);
        redis.opsForHash().increment(mTokensKey, model, tokens.longValueExact());
        applyExpire(mCallsKey, windowStart);
        applyExpire(mTokensKey, windowStart);
    }

    private String keyBase(UUID tenantId, UUID customerId, Instant windowStart) {
        long startEpoch = windowStart.getEpochSecond();
        return "usage:win:" + tenantId + ":" + customerId + ":" + startEpoch;
    }

    private void applyExpire(String key, Instant windowStart) {
        int winSec = (int) windowCalculator.windowSize().getSeconds();
        int hotTtl = resolveHotTtlSeconds();
        Instant expireAt = windowStart.plusSeconds(winSec + hotTtl);
        redis.expireAt(key, java.util.Date.from(expireAt));
    }

    private int resolveHotTtlSeconds() {
        var agg = properties.aggregation();
        int ttl = agg == null ? 300 : agg.hotTtlSeconds();
        return ttl > 0 ? ttl : 300;
    }
}
