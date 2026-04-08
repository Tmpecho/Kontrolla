CREATE TABLE audit_events
(
    id             CHAR(36)     NOT NULL,
    action         VARCHAR(64)  NOT NULL,
    outcome        VARCHAR(32)  NOT NULL,
    occurred_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_type     VARCHAR(32)  NOT NULL,
    actor_user_id  CHAR(36)     NULL,
    actor_email    VARCHAR(320) NULL,
    organization_id CHAR(36)    NULL,
    target_type    VARCHAR(32)  NULL,
    target_id      CHAR(36)     NULL,
    request_method VARCHAR(16)  NULL,
    request_path   VARCHAR(255) NULL,
    client_ip      VARCHAR(64)  NULL,
    user_agent     VARCHAR(512) NULL,
    result_code    VARCHAR(64)  NOT NULL,
    metadata_json  LONGTEXT     NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at);
CREATE INDEX idx_audit_events_action_occurred_at
    ON audit_events (action, occurred_at);
CREATE INDEX idx_audit_events_actor_user_id_occurred_at
    ON audit_events (actor_user_id, occurred_at);
CREATE INDEX idx_audit_events_organization_id_occurred_at
    ON audit_events (organization_id, occurred_at);
