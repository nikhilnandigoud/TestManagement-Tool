CREATE TABLE IF NOT EXISTS integration_credentials (
    id VARCHAR(128) PRIMARY KEY,
    jira_workspace_url VARCHAR(512),
    jira_auth_method VARCHAR(32),
    jira_token TEXT,
    jira_token_expiry VARCHAR(64),
    github_token TEXT,
    github_token_expiry VARCHAR(64),
    gitlab_token TEXT,
    gitlab_token_expiry VARCHAR(64),
    user_id VARCHAR(128),
    created_at BIGINT,
    updated_at BIGINT,
    is_team_level BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS integration_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(128),
    action VARCHAR(64),
    ticket_or_pr_id VARCHAR(512),
    source_system VARCHAR(32),
    success BOOLEAN,
    error_message TEXT,
    timestamp TIMESTAMP,
    response_time_ms BIGINT,
    ip_address VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_integration_audit_user_timestamp
    ON integration_audit_logs(user_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_integration_audit_source_timestamp
    ON integration_audit_logs(source_system, timestamp DESC);

CREATE TABLE IF NOT EXISTS integration_cache_entries (
    cache_key VARCHAR(1024) PRIMARY KEY,
    source_system VARCHAR(32) NOT NULL,
    payload_json TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_integration_cache_expires_at
    ON integration_cache_entries(expires_at);
