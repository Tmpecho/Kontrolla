ALTER TABLE documents
    ADD COLUMN file_name VARCHAR(255) NOT NULL DEFAULT 'document.pdf',
    ADD COLUMN content_type VARCHAR(255) NOT NULL DEFAULT 'application/pdf',
    ADD COLUMN file_size_bytes BIGINT NOT NULL DEFAULT 0;

CREATE TABLE document_files
(
    document_id CHAR(36) NOT NULL,
    content     LONGBLOB NOT NULL,
    PRIMARY KEY (document_id),
    CONSTRAINT fk_document_files_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);
