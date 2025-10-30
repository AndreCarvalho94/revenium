-- Flyway V1: Initial schema
-- Tenants
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenants_name ON tenants(name);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    external_id VARCHAR(150) NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_customers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_customer_tenant ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customer_external_id ON customers(external_id);

-- Usage Events
CREATE TABLE IF NOT EXISTS usage_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_usage_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_usage_events_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT uk_usage_event_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_usage_event_tenant ON usage_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_event_customer ON usage_events(customer_id);
CREATE INDEX IF NOT EXISTS idx_usage_event_timestamp ON usage_events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_usage_event_tenant_customer_ts ON usage_events(tenant_id, customer_id, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_usage_event_endpoint ON usage_events(endpoint);

-- Aggregation Windows
CREATE TABLE IF NOT EXISTS aggregation_windows (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    aggregations JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_agg_window_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agg_window_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT uk_aggwindow_tenant_customer_start UNIQUE (tenant_id, customer_id, window_start)
);

CREATE INDEX IF NOT EXISTS idx_aggwindow_tenant ON aggregation_windows(tenant_id);
CREATE INDEX IF NOT EXISTS idx_aggwindow_customer ON aggregation_windows(customer_id);
CREATE INDEX IF NOT EXISTS idx_aggwindow_start_end ON aggregation_windows(window_start, window_end);

