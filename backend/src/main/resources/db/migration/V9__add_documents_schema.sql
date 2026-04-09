CREATE TABLE documents
(
    id                 CHAR(36)     NOT NULL,
    organization_id    CHAR(36)     NOT NULL,
    establishment_id   CHAR(36)     NOT NULL,
    created_by_user_id CHAR(36)     NOT NULL,
    service_area       VARCHAR(32)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    holder_name        VARCHAR(255) NOT NULL,
    issue_date         DATE         NOT NULL,
    renewal_date       DATE         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_documents_org_est_service_created_at
    ON documents (organization_id, establishment_id, service_area, created_at);
CREATE INDEX idx_documents_est_service_renewal_date
    ON documents (establishment_id, service_area, renewal_date);
CREATE INDEX idx_documents_created_by_user_id
    ON documents (created_by_user_id);
