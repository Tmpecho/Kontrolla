CREATE TABLE temperature_units
(
    id                  CHAR(36)       NOT NULL,
    organization_id     CHAR(36)       NOT NULL,
    establishment_id    CHAR(36)       NOT NULL,
    name                VARCHAR(255)   NOT NULL,
    location            VARCHAR(255)   NOT NULL,
    type                VARCHAR(32)    NOT NULL,
    due_by_time         TIME           NOT NULL,
    minimum_temperature DECIMAL(6, 2)  NOT NULL,
    maximum_temperature DECIMAL(6, 2)  NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_temperature_units_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_temperature_units_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE
);

CREATE INDEX idx_temperature_units_org_est_name
    ON temperature_units (organization_id, establishment_id, name);
CREATE INDEX idx_temperature_units_est_due_by_time
    ON temperature_units (establishment_id, due_by_time);

CREATE TABLE temperature_logs
(
    id                  CHAR(36)       NOT NULL,
    temperature_unit_id CHAR(36)       NOT NULL,
    measured_at         TIMESTAMP      NOT NULL,
    temperature_celsius DECIMAL(6, 2)  NOT NULL,
    note                VARCHAR(1000)  NULL,
    logged_by_user_id   CHAR(36)       NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_temperature_logs_temperature_unit
        FOREIGN KEY (temperature_unit_id) REFERENCES temperature_units (id) ON DELETE CASCADE,
    CONSTRAINT fk_temperature_logs_logged_by_user
        FOREIGN KEY (logged_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_temperature_logs_unit_measured_at
    ON temperature_logs (temperature_unit_id, measured_at);
CREATE INDEX idx_temperature_logs_logged_by_user_id
    ON temperature_logs (logged_by_user_id);
