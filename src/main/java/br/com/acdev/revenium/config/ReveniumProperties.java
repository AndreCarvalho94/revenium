package br.com.acdev.revenium.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "revenium")
public record ReveniumProperties(AggregationProperties aggregation) {

    public record AggregationProperties(int windowSeconds) {}
}

