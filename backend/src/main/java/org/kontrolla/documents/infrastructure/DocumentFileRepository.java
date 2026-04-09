package org.kontrolla.documents.infrastructure;

import org.kontrolla.documents.domain.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {
}
