package br.com.acdev.revenium.domain.mapper;

import br.com.acdev.revenium.config.JsonHelper;
import br.com.acdev.revenium.controller.dto.UsageEventRequest;
import br.com.acdev.revenium.domain.entity.Customer;
import br.com.acdev.revenium.domain.entity.Tenant;
import br.com.acdev.revenium.domain.entity.UsageEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class UsageEventMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", expression = "java(toTenant(request.tenantId()))")
    @Mapping(target = "customer", expression = "java(toCustomer(request.customerId()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.parse(request.timestamp()))")
    @Mapping(target = "tenantId", expression = "java(java.util.UUID.fromString(request.tenantId()))")
    @Mapping(target = "customerId", expression = "java(java.util.UUID.fromString(request.customerId()))")
    @Mapping(target = "metadata", expression = "java(convertMetadataToJson(request.metadata(), jsonHelper))")
    public abstract UsageEvent toEntity(UsageEventRequest request, JsonHelper jsonHelper);

    protected String convertMetadataToJson(Map<String, Object> metadata, JsonHelper jsonHelper) {
        return jsonHelper.toJson(metadata);
    }

    protected Tenant toTenant(String tenantId) {
        Tenant t = new Tenant();
        t.setId(UUID.fromString(tenantId));
        return t;
    }

    protected Customer toCustomer(String customerId) {
        Customer c = new Customer();
        c.setId(UUID.fromString(customerId));
        return c;
    }
}
