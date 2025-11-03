package br.com.acdev.revenium.components;

import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("unused")
public class KeyBaseBuilder {

    public static final String OPEN_WINDOWS_KEY = "usage:open_windows";

    private KeyBaseBuilder() {
        // utility class
    }

    public static String execute(UUID tenantId, UUID customerId, Instant windowStart) {
        long startEpoch = windowStart.getEpochSecond();
        return "usage:win:" + tenantId + ":" + customerId + ":" + startEpoch;
    }

    public static String summaryKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return execute(tenantId, customerId, windowStart) + ":summary";
    }

    public static String byEndpointCallsKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return execute(tenantId, customerId, windowStart) + ":byEndpoint:calls";
    }

    public static String byEndpointTokensKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return execute(tenantId, customerId, windowStart) + ":byEndpoint:tokens";
    }

    public static String byModelCallsKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return execute(tenantId, customerId, windowStart) + ":byModel:calls";
    }

    public static String byModelTokensKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return execute(tenantId, customerId, windowStart) + ":byModel:tokens";
    }

    public static WindowKeyInfo parseWindowKey(String windowKey) {
        if (windowKey == null) return null;
        String[] parts = windowKey.split(":");
        if (parts.length < 5) return null;
        try {
            UUID tenantId = UUID.fromString(parts[2]);
            UUID customerId = UUID.fromString(parts[3]);
            long startEpoch = Long.parseLong(parts[4]);
            Instant windowStart = Instant.ofEpochSecond(startEpoch);
            return new WindowKeyInfo(tenantId, customerId, windowStart, windowKey);
        } catch (Exception e) {
            return null;
        }
    }

    public record WindowKeyInfo(UUID tenantId, UUID customerId, Instant windowStart, String raw) {}

}
