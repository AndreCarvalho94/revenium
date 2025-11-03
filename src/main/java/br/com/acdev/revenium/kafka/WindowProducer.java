package br.com.acdev.revenium.kafka;

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
public class WindowProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public static final String TOPIC_WINDOW_CLOSE = "usage.window.close";

    public boolean publishWindow(String windowKey) {
        String payload = toPayload(windowKey);
        try {
            SendResult<String, String> r = kafkaTemplate.send(TOPIC_WINDOW_CLOSE, windowKey, payload).get(5, TimeUnit.SECONDS);
            RecordMetadata md = r.getRecordMetadata();
            log.info("Published window {} to topic {} partition={} offset={} at {}",
                    windowKey, TOPIC_WINDOW_CLOSE, md.partition(), md.offset(), Instant.ofEpochMilli(md.timestamp()));
            return true;
        } catch (Exception e) {
            log.error("Failed to publish window {} to Kafka topic {}: {}", windowKey, TOPIC_WINDOW_CLOSE, e.toString());
            return false;
        }
    }

    private String toPayload(String windowKey) {
        return "{\"windowKey\":\"" + windowKey + "\"}";
    }
}

