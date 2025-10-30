package br.com.acdev.revenium.repository;

import br.com.acdev.revenium.domain.entity.AggregationWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AggregationWindowRepository extends JpaRepository<AggregationWindow, UUID> {
    Optional<AggregationWindow> findByTenantIdAndCustomerIdAndWindowStart(UUID tenantId, UUID customerId, Instant windowStart);
    List<AggregationWindow> findByTenantIdAndCustomerIdAndWindowStartBetween(UUID tenantId, UUID customerId, Instant start, Instant end);
    List<AggregationWindow> findByTenantId(UUID tenantId);
}

