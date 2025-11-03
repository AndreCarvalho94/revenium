package br.com.acdev.revenium.scheduler;

import br.com.acdev.revenium.components.KeyBaseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class WindowScheduler {

    private final StringRedisTemplate redis;

    // poll zset for windows to close every 5 seconds (configurable via property scheduler.poll-ms)
    @Scheduled(fixedDelayString = "${scheduler.poll-ms:5000}", initialDelayString = "${scheduler.initial-delay-ms:2000}")
    public void pollOpenWindows() {
        String zkey = KeyBaseBuilder.OPEN_WINDOWS_KEY;
        try {
            Set<String> items = redis.opsForZSet().range(zkey, 0, 10);
            if (items == null || items.isEmpty()) {
                log.debug("No open windows found in zset {}", zkey);
                return;
            }
            for (String item : items) {
                try {
                    Long removed = redis.opsForZSet().remove(zkey, item);
                    if (removed != null && removed > 0) {
                        log.info("Scheduler popped window from zset {} -> {}", zkey, item);
                        // here we would publish to Kafka or enqueue for processing
                    } else {
                        log.debug("Window {} was already removed by another instance", item);
                    }
                } catch (Throwable t) {
                    log.error("Failed to remove/process window {} from zset {}: {}", item, zkey, t.toString());
                }
            }
        } catch (Throwable t) {
            log.error("Error while polling open windows zset {}: {}", zkey, t.toString());
        }
    }
}
