package br.com.acdev.revenium.service;

import br.com.acdev.revenium.domain.Customer;
import br.com.acdev.revenium.domain.Tenant;
import br.com.acdev.revenium.domain.UsageEvent;
import br.com.acdev.revenium.repository.UsageEventRepository;
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
public class UsageEventService {

    private final UsageEventRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public UsageEventService(UsageEventRepository repository) {
        this.repository = repository;
    }

    public UsageEvent create(UUID tenantId,
                             UUID customerId,
                             String eventId,
                             Instant timestamp,
                             String apiEndpoint,
                             String metadata) {
        UsageEvent event = new UsageEvent();
        Tenant tenantRef = entityManager.getReference(Tenant.class, tenantId);
        Customer customerRef = entityManager.getReference(Customer.class, customerId);
        event.setTenant(tenantRef);
        event.setCustomer(customerRef);
        event.setEventId(eventId);
        event.setTimestamp(timestamp);
        event.setApiEndpoint(apiEndpoint);
        event.setMetadata(metadata);
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public Optional<UsageEvent> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<UsageEvent> findByEventId(String eventId) {
        return repository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<UsageEvent> findByTenantAndCustomerBetween(UUID tenantId, UUID customerId, Instant start, Instant end) {
        return repository.findByTenantIdAndCustomerIdAndTimestampBetween(tenantId, customerId, start, end);
    }

    public UsageEvent save(UsageEvent event) {
        return repository.save(event);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
