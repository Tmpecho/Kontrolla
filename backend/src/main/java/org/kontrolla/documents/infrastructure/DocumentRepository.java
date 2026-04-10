package org.kontrolla.documents.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for document metadata queries scoped by establishment and service area. */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  /**
   * Returns a page of documents for an establishment and service area.
   *
   * @param establishmentId the establishment identifier
   * @param organizationId the organization identifier
   * @param serviceArea the document service area
   * @param pageable pagination information
   * @return a page of matching documents
   */
  @EntityGraph(
      attributePaths = {
        "organization",
        "establishment",
        "createdByUser",
        "auditAssignments",
        "auditAssignments.user"
      })
  Page<Document> findByEstablishmentIdAndOrganizationIdAndServiceArea(
      UUID establishmentId,
      UUID organizationId,
      DocumentServiceArea serviceArea,
      Pageable pageable);

  /**
   * Finds a document by id scoped to an establishment and organization.
   *
   * @param id the document identifier
   * @param establishmentId the establishment identifier
   * @param organizationId the organization identifier
   * @return the matching document, if present
   */
  @EntityGraph(
      attributePaths = {
        "organization",
        "establishment",
        "createdByUser",
        "auditAssignments",
        "auditAssignments.user"
      })
  Optional<Document> findByIdAndEstablishmentIdAndOrganizationId(
      UUID id, UUID establishmentId, UUID organizationId);

  /**
   * Returns documents for an establishment and service area ordered by title.
   *
   * @param establishmentId the establishment identifier
   * @param organizationId the organization identifier
   * @param serviceArea the document service area
   * @return the matching documents
   */
  @EntityGraph(
      attributePaths = {
        "organization",
        "establishment",
        "createdByUser",
        "auditAssignments",
        "auditAssignments.user"
      })
  List<Document> findByEstablishmentIdAndOrganizationIdAndServiceAreaOrderByTitleAsc(
      UUID establishmentId, UUID organizationId, DocumentServiceArea serviceArea);
}
