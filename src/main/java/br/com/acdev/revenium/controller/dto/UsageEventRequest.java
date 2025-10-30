package br.com.acdev.revenium.controller.dto;

import java.util.Map;

public record UsageEventRequest(
    String eventId,
    String timestamp,
    String tenantId,
    String customerId,
    String apiEndpoint,
    Map<String, Object> metadata
) {}
