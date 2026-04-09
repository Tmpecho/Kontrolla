CREATE TABLE document_audit_assignments
(
    id              CHAR(36)  NOT NULL,
    document_id     CHAR(36)  NOT NULL,
    user_id         CHAR(36)  NOT NULL,
    acknowledged_at TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_document_audit_assignments_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_audit_assignments_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_document_audit_assignments_document_user
        UNIQUE (document_id, user_id)
);

CREATE INDEX idx_document_audit_assignments_document_id
    ON document_audit_assignments (document_id);

CREATE INDEX idx_document_audit_assignments_user_id
    ON document_audit_assignments (user_id);

CREATE INDEX idx_document_audit_assignments_acknowledged_at
    ON document_audit_assignments (acknowledged_at);
