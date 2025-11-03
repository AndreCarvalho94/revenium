package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.JsonHelper;
import br.com.acdev.revenium.components.UsageAccumulator;
import br.com.acdev.revenium.components.WindowCalculator;
import br.com.acdev.revenium.domain.Metadata;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.repository.UsageEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEventService {

    private final UsageEventRepository repository;
    private final JsonHelper jsonHelper;
    private final UsageAccumulator accumulator;
    private final WindowCalculator windowCalculator;

    public UsageEvent create(UsageEvent event) {
        Metadata metadata = jsonHelper.toObject(event.getMetadata(), Metadata.class);
        // Determine if the event belongs to a past (already closed) window
        Instant eventWindowStart = windowCalculator.windowStart(event.getTimestamp());
        Instant currentWindowStart = windowCalculator.windowStart(Instant.now());
        boolean isLate = eventWindowStart.isBefore(currentWindowStart);
        if (isLate) {
            // Ignore for hot path (Redis) but persist the raw event for later reprocessing
            log.debug("Ignoring late event for aggregation. eventId={}, eventTs={}, eventWindowStart={}, currentWindowStart={}",
                    event.getEventId(), event.getTimestamp(), eventWindowStart, currentWindowStart);
        } else {
            accumulator.accumulate(event, metadata);
        }
        return repository.save(event);
    }
}
