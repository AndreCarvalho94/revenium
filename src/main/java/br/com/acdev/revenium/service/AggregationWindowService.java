package br.com.acdev.revenium.service;

import br.com.acdev.revenium.domain.AggregationWindow;
import br.com.acdev.revenium.domain.Customer;
import br.com.acdev.revenium.domain.Tenant;
import br.com.acdev.revenium.repository.AggregationWindowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AggregationWindowService {

    private final AggregationWindowRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public AggregationWindowService(AggregationWindowRepository repository) {
        this.repository = repository;
    }

    public AggregationWindow create(UUID tenantId,
                                    UUID customerId,
                                    Instant windowStart,
                                    Instant windowEnd,
                                    String aggregations) {
        AggregationWindow window = new AggregationWindow();
        Tenant tenantRef = entityManager.getReference(Tenant.class, tenantId);
        Customer customerRef = entityManager.getReference(Customer.class, customerId);
        window.setTenant(tenantRef);
        window.setCustomer(customerRef);
        window.setWindowStart(windowStart);
        window.setWindowEnd(windowEnd);
        window.setAggregations(aggregations);
        return repository.save(window);
    }

    @Transactional(readOnly = true)
    public Optional<AggregationWindow> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<AggregationWindow> findByCompositeKey(UUID tenantId, UUID customerId, Instant windowStart) {
        return repository.findByTenantIdAndCustomerIdAndWindowStart(tenantId, customerId, windowStart);
    }

    @Transactional(readOnly = true)
    public List<AggregationWindow> findByTenantAndCustomerBetween(UUID tenantId, UUID customerId, Instant start, Instant end) {
        return repository.findByTenantIdAndCustomerIdAndWindowStartBetween(tenantId, customerId, start, end);
    }

    public AggregationWindow save(AggregationWindow window) {
        return repository.save(window);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
