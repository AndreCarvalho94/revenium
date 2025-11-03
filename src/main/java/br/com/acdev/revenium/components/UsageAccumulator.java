package br.com.acdev.revenium.components;

import br.com.acdev.revenium.config.ReveniumProperties;
import br.com.acdev.revenium.domain.Metadata;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;


@Component
@RequiredArgsConstructor
@Slf4j
public class UsageAccumulator {

    private final StringRedisTemplate redis;
    private final WindowCalculator windowCalculator;
    private final ReveniumProperties properties;


    public void accumulate(UsageEvent event, Metadata metadata) {
        Instant windowStart = windowCalculator.windowStart(event.getTimestamp());
        Instant windowEnd = windowCalculator.windowEndFromStart(windowStart);
        String base = KeyBaseBuilder.execute(event.getTenantId(), event.getCustomerId(), windowStart);
        String openWindowsKey = KeyBaseBuilder.OPEN_WINDOWS_KEY;
        long score = windowEnd.getEpochSecond();
        redis.opsForZSet().add(openWindowsKey, base, score);
        log.info("Marked window as open in zset key={} score={} base={}", openWindowsKey, score, base);

        String summaryKey = KeyBaseBuilder.summaryKey(event.getTenantId(), event.getCustomerId(), windowStart);
        accumulateSummaryKey(summaryKey, windowStart,
                metadata.tokens(), metadata.inputTokens(), metadata.outputTokens(), metadata.latencyMs());

        String epCallsKey = KeyBaseBuilder.byEndpointCallsKey(event.getTenantId(), event.getCustomerId(), windowStart);
        String epTokensKey = KeyBaseBuilder.byEndpointTokensKey(event.getTenantId(), event.getCustomerId(), windowStart);
        accumulateByEndpointKeys(epCallsKey, epTokensKey, windowStart, event.getApiEndpoint(), metadata.tokens());

        String mCallsKey = KeyBaseBuilder.byModelCallsKey(event.getTenantId(), event.getCustomerId(), windowStart);
        String mTokensKey = KeyBaseBuilder.byModelTokensKey(event.getTenantId(), event.getCustomerId(), windowStart);
        accumulateByModelKeys(mCallsKey, mTokensKey, windowStart, metadata.model(), metadata.tokens());
    }


    private void accumulateSummaryKey(String summaryKey, Instant windowStart,
                                      BigInteger tokens, BigInteger inputTokens, BigInteger outputTokens, BigInteger latencyMs) {
        redis.opsForHash().increment(summaryKey, "totalCalls", 1);
        redis.opsForHash().increment(summaryKey, "totalTokens", tokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalInputTokens", inputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalOutputTokens", outputTokens.longValueExact());
        redis.opsForHash().increment(summaryKey, "totalLatencyMs", latencyMs.longValueExact());
        applyExpire(summaryKey, windowStart);
    }

    private void accumulateByEndpointKeys(String epCallsKey, String epTokensKey, Instant windowStart, String endpoint, BigInteger tokens) {
        redis.opsForHash().increment(epCallsKey, endpoint, 1);
        redis.opsForHash().increment(epTokensKey, endpoint, tokens.longValueExact());
        applyExpire(epCallsKey, windowStart);
        applyExpire(epTokensKey, windowStart);
    }

    private void accumulateByModelKeys(String mCallsKey, String mTokensKey, Instant windowStart, String model, BigInteger tokens) {
        redis.opsForHash().increment(mCallsKey, model, 1);
        redis.opsForHash().increment(mTokensKey, model, tokens.longValueExact());
        applyExpire(mCallsKey, windowStart);
        applyExpire(mTokensKey, windowStart);
    }

    private void applyExpire(String key, Instant windowStart) {
        long winSec = windowCalculator.windowSize().getSeconds();
        long total = winSec + resolveHotTtlSeconds();
        Instant expireAt = windowStart.plusSeconds(total);
        redis.expireAt(key, java.util.Date.from(expireAt));
    }

    private int resolveHotTtlSeconds() {
        var agg = properties.aggregation();
        int ttl = agg == null ? 300 : agg.hotTtlSeconds();
        return ttl > 0 ? ttl : 300;
    }
}
