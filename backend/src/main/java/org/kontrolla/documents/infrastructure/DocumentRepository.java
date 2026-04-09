package org.kontrolla.documents.infrastructure;

import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

  @EntityGraph(attributePaths = {"organization", "establishment", "createdByUser"})
  Page<Document> findByEstablishmentIdAndOrganizationIdAndServiceArea(
      UUID establishmentId,
      UUID organizationId,
      DocumentServiceArea serviceArea,
      Pageable pageable
  );

  @EntityGraph(attributePaths = {"organization", "establishment", "createdByUser"})
  Optional<Document> findByIdAndEstablishmentIdAndOrganizationId(
      UUID id,
      UUID establishmentId,
      UUID organizationId
  );
}
