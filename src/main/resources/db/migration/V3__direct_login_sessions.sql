CREATE TABLE login_session (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 refresh_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_login_session_user ON login_session(user_id);
CREATE INDEX idx_login_session_expiry ON login_session(expires_at);
