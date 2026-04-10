package org.kontrolla.documents.infrastructure;

import java.util.UUID;
import org.kontrolla.documents.domain.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for stored document file content. */
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {}
