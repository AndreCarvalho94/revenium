package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.KeyBaseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WindowService {

    private final StringRedisTemplate redis;

    /**
     * Claim up to maxItems windows from the open windows zset using ZPOPMIN (opsForZSet().popMin).
     * This relies on a modern Redis + client that supports ZPOPMIN; the operation is atomic per-item
     * so multiple scheduler instances won't claim the same member.
     *
     * If an unexpected error occurs, we log and return an empty set (no fallback removal will be attempted).
     */
    public Set<String> claimOpenWindows(int maxItems) {
        String zkey = KeyBaseBuilder.OPEN_WINDOWS_KEY;
        try {
            Set<String> claimed = new HashSet<>();
            for (int i = 0; i < maxItems; i++) {
                ZSetOperations.TypedTuple<String> t = redis.opsForZSet().popMin(zkey);
                if (t == null) break;
                String member = t.getValue();
                if (member != null) {
                    claimed.add(member);
                }
            }
            return claimed;
        } catch (Exception e) {
            log.error("Failed to claim windows using ZPOPMIN on {}: {}", zkey, e.toString());
            return Set.of();
        }
    }
}
