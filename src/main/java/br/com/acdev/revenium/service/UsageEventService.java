package br.com.acdev.revenium.service;

import br.com.acdev.revenium.config.JsonHelper;
import br.com.acdev.revenium.domain.Metadata;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.repository.UsageEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageEventService {

    private final UsageEventRepository repository;
    private final WindowCalculator windowCalculator;
    private final JsonHelper jsonHelper;

    public UsageEvent save(UsageEvent event) {
        Metadata metadata = jsonHelper.toObject(event.getMetadata(), Metadata.class);
        return repository.save(event);
    }
}
