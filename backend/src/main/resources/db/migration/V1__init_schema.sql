-- ADR-002: Schema Relacional Transacional gerenciado pelo Flyway (PostgreSQL)

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    plan_tier VARCHAR(40) NOT NULL DEFAULT 'PRIME_GOLD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    category VARCHAR(80) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    stock INTEGER NOT NULL,
    rating DOUBLE PRECISION NOT NULL DEFAULT 4.8,
    aem_content_path VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(64) NOT NULL UNIQUE,
    user_email VARCHAR(160) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL
);

CREATE TABLE navigation_audit (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(80) NOT NULL,
    user_email VARCHAR(160) NOT NULL,
    action_type VARCHAR(80) NOT NULL,
    resource_path VARCHAR(255) NOT NULL,
    latency_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_user_email ON orders(user_email);
CREATE INDEX idx_nav_audit_created ON navigation_audit(created_at);

