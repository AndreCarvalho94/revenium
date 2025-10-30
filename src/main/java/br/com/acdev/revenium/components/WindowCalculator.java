package br.com.acdev.revenium.components;

import br.com.acdev.revenium.config.ReveniumProperties;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;


@Component
public class WindowCalculator {

    private final ReveniumProperties properties;

    public WindowCalculator(ReveniumProperties properties) {
        this.properties = properties;
    }

    private int windowSeconds() {
        ReveniumProperties.AggregationProperties agg = properties.aggregation();
        int seconds = agg == null ? 30 : agg.windowSeconds();
        return seconds > 0 ? seconds : 30;
    }

    public Duration windowSize() {
        return Duration.ofSeconds(windowSeconds());
    }


    public Instant windowStart(UsageEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return windowStart(event.getTimestamp());
    }


    public Instant windowStart(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        long epochSec = timestamp.getEpochSecond();
        int win = windowSeconds();
        long startSec = Math.floorDiv(epochSec, win) * win;
        return Instant.ofEpochSecond(startSec);
    }

    public Instant windowEnd(Instant timestamp) {
        return windowStart(timestamp).plusSeconds(windowSeconds());
    }

    public Instant windowEndFromStart(Instant windowStart) {
        Objects.requireNonNull(windowStart, "windowStart must not be null");
        return windowStart.plusSeconds(windowSeconds());
    }
}
