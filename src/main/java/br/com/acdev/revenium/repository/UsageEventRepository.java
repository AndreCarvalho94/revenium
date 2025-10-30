package br.com.acdev.revenium.repository;

import br.com.acdev.revenium.domain.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
    Optional<UsageEvent> findByEventId(String eventId);
    List<UsageEvent> findByTenantIdAndCustomerIdAndTimestampBetween(UUID tenantId, UUID customerId, Instant start, Instant end);
    List<UsageEvent> findByTenantId(UUID tenantId);
}

