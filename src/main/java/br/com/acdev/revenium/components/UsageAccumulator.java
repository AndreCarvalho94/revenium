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
public class UsageAccumulator {

    private final StringRedisTemplate redis;
    private final WindowCalculator windowCalculator;
    private final ReveniumProperties properties;


    public void accumulate(UsageEvent event, Metadata metadata) {
        Instant windowStart = windowCalculator.windowStart(event.getTimestamp());
        String base = KeyBaseBuilder.execute(event.getTenantId(), event.getCustomerId(), windowStart);

        accumulateSummary(base, windowStart,
                metadata.tokens(), metadata.inputTokens(), metadata.outputTokens(), metadata.latencyMs());

        accumulateByEndpoint(base, windowStart, event.getApiEndpoint(), metadata.tokens());
        accumulateByModel(base, windowStart, metadata.model(), metadata.tokens());
    }


    private void accumulateSummary(String base, Instant windowStart,
                                   BigInteger tokens, BigInteger inputTokens, BigInteger outputTokens, BigInteger latencyMs) {
        String summaryKey = base + ":summary";
        redis.opsForHash().increment(summaryKey, "totalCalls", 1);
        redis.opsForHash().increment(summaryKey, "totalTokens", tokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalInputTokens", inputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalOutputTokens", outputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalLatencyMs", latencyMs.longValueExact());
        applyExpire(summaryKey, windowStart);
    }

    private void accumulateByEndpoint(String base, Instant windowStart, String endpoint, BigInteger tokens) {
        String epCallsKey = base + ":byEndpoint:calls";
        String epTokensKey = base + ":byEndpoint:tokens";
        redis.opsForHash().increment(epCallsKey, endpoint, 1);
        redis.opsForHash().increment(epTokensKey, endpoint, tokens.longValueExact());
        applyExpire(epCallsKey, windowStart);
        applyExpire(epTokensKey, windowStart);
    }

    private void accumulateByModel(String base, Instant windowStart, String model, BigInteger tokens) {
        String mCallsKey = base + ":byModel:calls";
        String mTokensKey = base + ":byModel:tokens";
        redis.opsForHash().increment(mCallsKey, model, 1);
        redis.opsForHash().increment(mTokensKey, model, tokens.longValueExact());
        applyExpire(mCallsKey, windowStart);
        applyExpire(mTokensKey, windowStart);
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
