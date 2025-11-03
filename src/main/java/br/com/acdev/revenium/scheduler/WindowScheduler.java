package br.com.acdev.revenium.scheduler;

import br.com.acdev.revenium.components.KeyBaseBuilder;
import br.com.acdev.revenium.kafka.WindowCloseProducer;
import br.com.acdev.revenium.service.ClaimReadyWindowToClose;
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

    private final ClaimReadyWindowToClose claimReadyWindowToClose;
    private final WindowCloseProducer producer;
    private final StringRedisTemplate redis;

    @Scheduled(fixedDelayString = "${scheduler.poll-ms:5000}", initialDelayString = "${scheduler.initial-delay-ms:2000}")
    public void pollOpenWindows() {
        Set<String> claimed = claimReadyWindowToClose.execute(10);
        if (claimed == null || claimed.isEmpty()) {
            log.info("No open windows claimed");
            return;
        }
        for (String window : claimed) {
            log.info("Claimed window to close: {}", window);
            boolean ok = producer.publishWindow(window);
            if (!ok) {
                long score = System.currentTimeMillis() / 1000L;
                redis.opsForZSet().add(KeyBaseBuilder.OPEN_WINDOWS_KEY, window, score);
                log.warn("Re-queued window {} after failed publish", window);
            }
        }
    }
}
