package br.com.acdev.revenium.domain;

import lombok.Builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

@Builder
public record Aggregations(
        BigInteger totalCalls,
        BigInteger totalTokens,
        BigInteger totalInputTokens,
        BigInteger totalOutputTokens,
        BigDecimal avgLatencyMs,
        Map<String, SubAggregation> byEndpoint,
        Map<String, SubAggregation> byModel
) {
    @Builder
    public record SubAggregation(BigInteger calls, BigInteger tokens) {}
}
