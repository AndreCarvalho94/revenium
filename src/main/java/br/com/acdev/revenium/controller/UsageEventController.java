package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.controller.dto.UsageEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usage-events")
public class UsageEventController {

    private static final Logger logger = LoggerFactory.getLogger(UsageEventController.class);

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody UsageEventRequest request) {
        logger.info("Received usage event: {}", request);
        return ResponseEntity.ok().build();
    }
}
