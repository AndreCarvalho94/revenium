package br.com.acdev.revenium.service;

import br.com.acdev.revenium.domain.entity.AggregationWindow;
import br.com.acdev.revenium.repository.AggregationWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AggregationWindowService {

    private final AggregationWindowRepository repository;

    public AggregationWindow save(AggregationWindow window) {
        return repository.save(window);
    }

}
