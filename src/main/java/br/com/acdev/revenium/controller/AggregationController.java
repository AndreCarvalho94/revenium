package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.domain.Aggregations;
import br.com.acdev.revenium.domain.entity.AggregationWindow;
import br.com.acdev.revenium.dto.AggregationWindowDto;
import br.com.acdev.revenium.service.AggregationWindowService;
import br.com.acdev.revenium.components.JsonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aggregations")
public class AggregationController {

    private final AggregationWindowService aggregationWindowService;
    private final JsonHelper jsonHelper;

    @GetMapping("/current")
    public ResponseEntity<Aggregations> read(@RequestParam UUID tenantId,
                                             @RequestParam UUID customerId) {
        Optional<Aggregations> opt = aggregationWindowService.readCurrentAggregation(tenantId, customerId);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<List<AggregationWindowDto>> read(@RequestParam UUID tenantId,
                                                           @RequestParam UUID customerId,
                                                           @RequestParam(required = false) Instant from,
                                                           @RequestParam(required = false) Instant to) {
        List<AggregationWindow> entities = aggregationWindowService.listPersistedAggregations(tenantId, customerId, from, to);
        if (entities == null || entities.isEmpty()) return ResponseEntity.noContent().build();
        List<AggregationWindowDto> dtos = entities.stream().map(e -> {
            Aggregations agg = null;
            try {
                agg = jsonHelper.toObject(e.getAggregations(), Aggregations.class);
            } catch (Exception ex) {
                // ignore parse error, leave agg null
            }
            return new AggregationWindowDto(e.getTenantId(), e.getCustomerId(), e.getWindowStart(), e.getWindowEnd(), agg);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
