CREATE TABLE otp_verifications (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 5,
    verified BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_verifications_phone_active ON otp_verifications(phone_number, verified, expires_at);
