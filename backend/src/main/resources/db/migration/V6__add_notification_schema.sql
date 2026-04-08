CREATE TABLE notifications
(
    id               CHAR(36)      NOT NULL,
    recipient_user_id CHAR(36)     NOT NULL,
    organization_id  CHAR(36)      NOT NULL,
    establishment_id CHAR(36)      NOT NULL,
    service_area     VARCHAR(32)   NOT NULL,
    type             VARCHAR(64)   NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    message          VARCHAR(2000) NOT NULL,
    resource_type    VARCHAR(32)   NOT NULL,
    resource_id      CHAR(36)      NOT NULL,
    read_at          TIMESTAMP     NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_notifications_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id)
);

CREATE INDEX idx_notifications_recipient_created_at
    ON notifications (recipient_user_id, created_at);
CREATE INDEX idx_notifications_recipient_read_at_created_at
    ON notifications (recipient_user_id, read_at, created_at);
CREATE INDEX idx_notifications_resource
    ON notifications (resource_type, resource_id);
