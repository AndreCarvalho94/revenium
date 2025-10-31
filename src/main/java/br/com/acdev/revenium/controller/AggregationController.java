package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.domain.Aggregations;
import br.com.acdev.revenium.service.AggregationWindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aggregations")
public class AggregationController {

    private final AggregationWindowService aggregationWindowService;

    @GetMapping("/current")
    public ResponseEntity<Aggregations> read(@RequestParam UUID tenantId,
                                             @RequestParam UUID customerId) {
        Optional<Aggregations> opt = aggregationWindowService.readCurrentAggregation(tenantId, customerId);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}

