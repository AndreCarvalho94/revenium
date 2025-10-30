package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.config.JsonHelper;
import br.com.acdev.revenium.controller.dto.UsageEventRequest;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import br.com.acdev.revenium.domain.mapper.UsageEventMapper;
import br.com.acdev.revenium.service.UsageEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/usage-events")
public class UsageEventController {


    private final UsageEventService usageEventService;
    private final UsageEventMapper usageEventMapper;
    private final JsonHelper jsonHelper;

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody UsageEventRequest request) {
        log.info("Received usage event: {}", request);
        UsageEvent event = usageEventMapper.toEntity(request, jsonHelper);
        usageEventService.save(event);
        return ResponseEntity.ok().build();
    }
}
