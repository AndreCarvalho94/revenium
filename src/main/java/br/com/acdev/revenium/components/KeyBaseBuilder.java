package br.com.acdev.revenium.components;

import java.time.Instant;
import java.util.UUID;

public class KeyBaseBuilder {

    public static String execute(UUID tenantId, UUID customerId, Instant windowStart) {
        long startEpoch = windowStart.getEpochSecond();
        return "usage:win:" + tenantId + ":" + customerId + ":" + startEpoch;
    }

}
