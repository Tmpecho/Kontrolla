CREATE TABLE deviations
(
    id                  CHAR(36)      NOT NULL,
    organization_id     CHAR(36)      NOT NULL,
    establishment_id    CHAR(36)      NOT NULL,
    created_by_user_id  CHAR(36)      NOT NULL,
    assigned_to_user_id CHAR(36)      NULL,
    title               VARCHAR(255)  NOT NULL,
    description         VARCHAR(2000) NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    severity            VARCHAR(32)   NOT NULL,
    category            VARCHAR(32)   NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_deviations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_deviations_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE,
    CONSTRAINT fk_deviations_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_deviations_assigned_to_user
        FOREIGN KEY (assigned_to_user_id) REFERENCES users (id)
);

CREATE INDEX idx_deviations_organization_created_at
    ON deviations (organization_id, created_at);
CREATE INDEX idx_deviations_organization_establishment_created_at
    ON deviations (organization_id, establishment_id, created_at);
CREATE INDEX idx_deviations_created_by_user_id
    ON deviations (created_by_user_id);
CREATE INDEX idx_deviations_assigned_to_user_id
    ON deviations (assigned_to_user_id);
