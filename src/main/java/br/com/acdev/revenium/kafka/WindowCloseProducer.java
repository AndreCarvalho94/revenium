package br.com.acdev.revenium.kafka;

import br.com.acdev.revenium.components.JsonHelper;
import br.com.acdev.revenium.components.KeyBaseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WindowCloseProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonHelper jsonHelper;

    public static final String TOPIC_WINDOW_CLOSE = "usage.window.close";

    public boolean publishWindow(String windowKey) {
        var info = KeyBaseBuilder.parseWindowKey(windowKey);
        WindowCloseEvent evt;
        if (info != null) {
            evt = new WindowCloseEvent(info.raw(), info.tenantId(), info.customerId(), info.windowStart());
        } else {
            // fallback: publish minimal info
            evt = new WindowCloseEvent(windowKey, null, null, Instant.now());
        }

        String payload = jsonHelper.toJson(evt);
        try {
            SendResult<String, String> r = kafkaTemplate.send(TOPIC_WINDOW_CLOSE, windowKey, payload).get(5, TimeUnit.SECONDS);
            RecordMetadata md = r.getRecordMetadata();
            log.info("Published window {} to topic {} partition={} offset={} at {}",
                    windowKey, TOPIC_WINDOW_CLOSE, md.partition(), md.offset(), Instant.ofEpochMilli(md.timestamp()));
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing window {} to Kafka topic {}", windowKey, TOPIC_WINDOW_CLOSE, ie);
            return false;
        } catch (Exception e) {
            log.error("Failed to publish window {} to Kafka topic {}: {}", windowKey, TOPIC_WINDOW_CLOSE, e.toString());
            return false;
        }
    }

}
