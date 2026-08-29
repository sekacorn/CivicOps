CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID
);

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_replacement
    FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_token(id);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
CREATE INDEX idx_refresh_token_active_user ON refresh_token(user_id, expires_at) WHERE revoked_at IS NULL;
