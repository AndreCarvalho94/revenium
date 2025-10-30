package br.com.acdev.revenium.service;

import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.repository.UsageEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageEventService {

    private final UsageEventRepository repository;

    public UsageEvent save(UsageEvent event) {
        return repository.save(event);
    }
}
