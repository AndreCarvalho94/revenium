package br.com.acdev.revenium.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowCloseEvent {
    private String windowKey;
    private UUID tenantId;
    private UUID customerId;
    private Instant windowStart;
}

