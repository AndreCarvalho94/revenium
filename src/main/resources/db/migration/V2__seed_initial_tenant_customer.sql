-- V2: Seed initial Tenant and Customer

-- Insert default tenant (id deterministico para reprodutibilidade)
INSERT INTO tenants (id, name, active, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Nebula',
    TRUE,
    now(),
    now()
)
ON CONFLICT (name) DO NOTHING;

-- Insert default customer linked to that tenant
INSERT INTO customers (id, tenant_id, external_id, name, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    (SELECT id FROM tenants WHERE name = 'Nebula' LIMIT 1),
    'default',
    'Aurora',
    now(),
    now()
)
ON CONFLICT DO NOTHING;
