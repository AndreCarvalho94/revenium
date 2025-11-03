package br.com.acdev.revenium.dto;

import br.com.acdev.revenium.domain.Aggregations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregationWindowDto {
    private UUID tenantId;
    private UUID customerId;
    private Instant windowStart;
    private Instant windowEnd;
    private Aggregations aggregations;
}

