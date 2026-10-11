CREATE TABLE async_jobs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    job_name VARCHAR(255),
    parameters JSONB NOT NULL,
    parameters_hash VARCHAR(64) NOT NULL,
    owner_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    estimated_duration_sec INT,
    result JSONB,
    priority INT NOT NULL DEFAULT 0,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    correlation_id VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Idempotency: identical in-flight requests reuse the same job
CREATE UNIQUE INDEX idx_async_jobs_dedup ON async_jobs(parameters_hash)
    WHERE status IN ('PENDING', 'RUNNING') AND deleted = FALSE;
CREATE INDEX idx_async_jobs_polling ON async_jobs(status, priority DESC, created_at) WHERE deleted = FALSE;
CREATE INDEX idx_async_jobs_owner_key ON async_jobs(owner_key);
CREATE INDEX idx_async_jobs_correlation_id ON async_jobs(correlation_id) WHERE correlation_id IS NOT NULL;
