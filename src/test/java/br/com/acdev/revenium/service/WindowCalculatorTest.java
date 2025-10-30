package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.WindowCalculator;
import br.com.acdev.revenium.config.ReveniumProperties;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowCalculatorTest {

    private WindowCalculator calc;

    @BeforeEach
    void setup() {
        // Define janela de 30s para os testes
        ReveniumProperties props = new ReveniumProperties(new ReveniumProperties.AggregationProperties(30));
        calc = new WindowCalculator(props);
    }

    @Test
    void windowStart_onExactZeroBoundary() {
        Instant ts = Instant.parse("2025-01-01T00:00:00Z");
        assertEquals(ts, calc.windowStart(ts));
    }

    @Test
    void windowStart_onExactThirtyBoundary() {
        Instant ts = Instant.parse("2025-01-01T10:15:30Z");
        assertEquals(ts, calc.windowStart(ts));
    }

    @Test
    void windowStart_withinSecondHalfOfMinute() {
        Instant ts = Instant.parse("2025-01-01T10:15:45Z");
        Instant expected = Instant.parse("2025-01-01T10:15:30Z");
        assertEquals(expected, calc.windowStart(ts));
    }

    @Test
    void windowStart_justBeforeThirtyBoundary() {
        Instant ts = Instant.parse("2025-01-01T10:15:29Z");
        Instant expected = Instant.parse("2025-01-01T10:15:00Z");
        assertEquals(expected, calc.windowStart(ts));
    }

    @Test
    void windowStart_handlesNegativeEpochCorrectly() {
        Instant ts = Instant.ofEpochSecond(-1); // 1969-12-31T23:59:59Z
        Instant expected = Instant.ofEpochSecond(-30); // 1969-12-31T23:59:30Z
        assertEquals(expected, calc.windowStart(ts));
    }

    @Test
    void windowEnd_matchesStartPlusWindow() {
        Instant ts = Instant.parse("2025-01-01T10:15:01Z");
        Instant start = Instant.parse("2025-01-01T10:15:00Z");
        Instant end = Instant.parse("2025-01-01T10:15:30Z");
        assertEquals(end, calc.windowEnd(ts));
        assertEquals(end, calc.windowEndFromStart(start));
    }

    @Test
    void windowStart_fromUsageEvent() {
        UsageEvent ev = new UsageEvent();
        ev.setTimestamp(Instant.parse("2025-01-01T12:00:17Z"));
        Instant expected = Instant.parse("2025-01-01T12:00:00Z");
        assertEquals(expected, calc.windowStart(ev));
    }
}

