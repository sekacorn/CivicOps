CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(300),
    description TEXT,
    organization_type VARCHAR(50) NOT NULL,
    email VARCHAR(320),
    phone VARCHAR(50),
    website VARCHAR(500),
    address_line1 VARCHAR(250),
    address_line2 VARCHAR(250),
    city VARCHAR(120),
    state VARCHAR(120),
    postal_code VARCHAR(30),
    country VARCHAR(2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE organization_membership (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_membership_organization_user UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_membership_user ON organization_membership(user_id);
