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

}
