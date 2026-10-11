CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deletion_scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number)
);

CREATE INDEX idx_users_deletion_scheduled ON users(deletion_scheduled_at)
    WHERE deleted = TRUE AND deletion_scheduled_at IS NOT NULL;
