package br.com.acdev.revenium.service;

import br.com.acdev.revenium.components.JsonHelper;
import br.com.acdev.revenium.components.UsageAccumulator;
import br.com.acdev.revenium.domain.Metadata;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.repository.UsageEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageEventService {

    private final UsageEventRepository repository;
    private final JsonHelper jsonHelper;
    private final UsageAccumulator accumulator;

    public UsageEvent create(UsageEvent event) {
        Metadata metadata = jsonHelper.toObject(event.getMetadata(), Metadata.class);
        accumulator.accumulate(event, metadata);
        return repository.save(event);
    }
}
