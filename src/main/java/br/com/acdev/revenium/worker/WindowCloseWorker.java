package br.com.acdev.revenium.worker;

import br.com.acdev.revenium.components.JsonHelper;
import br.com.acdev.revenium.components.KeyBaseBuilder;
import br.com.acdev.revenium.components.WindowCalculator;
import br.com.acdev.revenium.kafka.WindowCloseProducer;
import br.com.acdev.revenium.kafka.WindowCloseEvent;
import br.com.acdev.revenium.service.AggregationWindowService;
import br.com.acdev.revenium.domain.Aggregations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@Profile("worker")
@RequiredArgsConstructor
public class WindowCloseWorker {

    private final AggregationWindowService aggregationWindowService;
    private final JsonHelper jsonHelper;
    private final WindowCalculator windowCalculator;

    @KafkaListener(topics = WindowCloseProducer.TOPIC_WINDOW_CLOSE, groupId = "window-worker-group")
    public void handleWindowClose(String payload) {
        WindowCloseEvent evt;
        try {
            evt = jsonHelper.toObject(payload, WindowCloseEvent.class);
        } catch (Exception e) {
            log.warn("Could not parse WindowCloseEvent from payload: {}", payload);
            return;
        }

        log.info("Worker received window close for {} (raw payload={})", evt.getWindowKey(), payload);

        var info = KeyBaseBuilder.parseWindowKey(evt.getWindowKey());
        if (info == null) {
            log.warn("Unexpected windowKey format: {}", evt.getWindowKey());
            return;
        }

        try {
            var opt = aggregationWindowService.readAggregation(info.tenantId(), info.customerId(), info.windowStart());
            if (opt.isPresent()) {
                Aggregations agg = opt.get();
                Instant windowEnd = windowCalculator.windowEndFromStart(info.windowStart());
                aggregationWindowService.persistAggregation(info.tenantId(), info.customerId(), info.windowStart(), windowEnd, agg);
                log.info("Persisted aggregation for {}", info.raw());
            } else {
                log.warn("No aggregation found in Redis for window {} — nothing to persist", info.raw());
            }
        } catch (Exception e) {
            log.error("Failed to process windowKey {}", info.raw(), e);
        }
    }

}
