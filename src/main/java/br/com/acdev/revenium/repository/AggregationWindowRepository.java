package br.com.acdev.revenium.repository;

import br.com.acdev.revenium.domain.entity.AggregationWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AggregationWindowRepository extends JpaRepository<AggregationWindow, Long> {
    Optional<AggregationWindow> findByTenantIdAndCustomerIdAndWindowStart(UUID tenantId, UUID customerId, Instant windowStart);
}

