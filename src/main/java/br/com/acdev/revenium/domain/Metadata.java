package br.com.acdev.revenium.domain;

import lombok.Builder;

import java.math.BigInteger;

@Builder
public record Metadata(
        BigInteger tokens,
        String model,
        BigInteger latencyMs,
        BigInteger inputTokens,
        BigInteger outputTokens
) {}

