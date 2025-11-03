package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.KeyBaseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class WindowService {

    private final StringRedisTemplate redis;
    private static final String ZSET_KEY = KeyBaseBuilder.OPEN_WINDOWS_KEY;


    @SuppressWarnings("rawtypes")
    public Set<String> claimReadyWindows(int maxItems) {
        long nowSeconds = Instant.now().getEpochSecond();

        String lua = """
                local members = redis.call('ZRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[2], 'LIMIT', ARGV[3], ARGV[4])
                if not members or #members == 0 then return {} end
                local removed = {}
                for i, v in ipairs(members) do
                  local r = redis.call('ZREM', KEYS[1], v)
                  if r == 1 then table.insert(removed, v) end
                end
                return removed
                """;

        DefaultRedisScript<List> script = new DefaultRedisScript<>(lua, List.class);
        try {
            List<?> raw = redis.execute(script, Collections.singletonList(ZSET_KEY), "0", String.valueOf(nowSeconds), "0", String.valueOf(maxItems));
            if (raw.isEmpty()) return Collections.emptySet();
            Set<String> removed = new HashSet<>();
            for (Object o : raw) {
                removed.add(String.valueOf(o));
            }
            return removed;
        } catch (Exception e) {
            log.error("Atomic claim failed on {}:", ZSET_KEY, e);
            return Collections.emptySet();
        }
    }
}