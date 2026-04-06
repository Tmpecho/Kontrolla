CREATE TABLE user_invites
(
    id              CHAR(36)    NOT NULL,
    user_id         CHAR(36)    NOT NULL,
    organization_id CHAR(36)    NOT NULL,
    token_hash      VARCHAR(64) NOT NULL,
    expires_at      TIMESTAMP   NOT NULL,
    accepted_at     TIMESTAMP   NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_invites_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_invites_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_invites_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_invites_user_id ON user_invites (user_id);
