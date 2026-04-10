package org.kontrolla.documents.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentAuditAssignment;
import org.kontrolla.documents.domain.DocumentFile;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.documents.infrastructure.DocumentFileRepository;
import org.kontrolla.documents.infrastructure.DocumentRepository;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.application.UserAccessService;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles document lifecycle operations, file storage, and audit acknowledgements. */
@Service
public class DocumentService {

  private static final String PDF_CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;

  private final DocumentRepository documentRepository;
  private final DocumentFileRepository documentFileRepository;
  private final OrganizationAccessService organizationAccessService;
  private final EstablishmentService establishmentService;
  private final UserAccessService userAccessService;
  private final OrganizationMembershipRepository organizationMembershipRepository;
  private final Clock clock;

  /**
   * Creates a document service backed by document, file, and access services.
   *
   * @param documentRepository repository for document metadata
   * @param documentFileRepository repository for document file content
   * @param organizationAccessService service for organization access checks
   * @param establishmentService service for establishment access and lookup
   * @param userAccessService service for resolving users
   * @param organizationMembershipRepository repository for organization membership lookups
   * @param clock clock used for acknowledgement timestamps
   */
  public DocumentService(
      DocumentRepository documentRepository,
      DocumentFileRepository documentFileRepository,
      OrganizationAccessService organizationAccessService,
      EstablishmentService establishmentService,
      UserAccessService userAccessService,
      OrganizationMembershipRepository organizationMembershipRepository,
      Clock clock) {
    this.documentRepository = documentRepository;
    this.documentFileRepository = documentFileRepository;
    this.organizationAccessService = organizationAccessService;
    this.establishmentService = establishmentService;
    this.userAccessService = userAccessService;
    this.organizationMembershipRepository = organizationMembershipRepository;
    this.clock = clock;
  }

  /**
   * Lists documents for an establishment and service area.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param serviceArea the document service area
   * @param currentUser the authenticated user
   * @param pageable pagination information
   * @return a page of matching documents
   */
  @Transactional(readOnly = true)
  public Page<Document> listDocuments(
      UUID organizationId,
      UUID establishmentId,
      DocumentServiceArea serviceArea,
      CurrentUser currentUser,
      Pageable pageable) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    return documentRepository.findByEstablishmentIdAndOrganizationIdAndServiceArea(
        establishmentId, organizationId, serviceArea, pageable);
  }

  /**
   * Returns a single document after access validation.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param currentUser the authenticated user
   * @return the requested document
   */
  @Transactional(readOnly = true)
  public Document getDocument(
      UUID organizationId, UUID establishmentId, UUID documentId, CurrentUser currentUser) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    return findDocumentOrThrow(organizationId, establishmentId, documentId);
  }

  /**
   * Returns the stored file contents for a document.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param currentUser the authenticated user
   * @return the downloadable file payload
   */
  @Transactional(readOnly = true)
  public DocumentFileDownload getDocumentFile(
      UUID organizationId, UUID establishmentId, UUID documentId, CurrentUser currentUser) {
    Document document = getDocument(organizationId, establishmentId, documentId, currentUser);
    DocumentFile documentFile =
        documentFileRepository
            .findById(documentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "document_file_not_found", "Document file not found"));

    return new DocumentFileDownload(
        document.getFileName(),
        document.getContentType(),
        document.getFileSizeBytes(),
        documentFile.getContent());
  }

  /**
   * Creates a new document together with its stored PDF file and audit assignments.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param serviceArea the document service area
   * @param title the document title
   * @param holderName the document holder name
   * @param issueDate the issue date
   * @param renewalDate the renewal date
   * @param auditUserIds users assigned to acknowledge the document
   * @param fileName the uploaded file name
   * @param contentType the uploaded content type
   * @param fileContent the uploaded file content
   * @param currentUser the authenticated user
   * @return the created document
   */
  @Transactional
  public Document createDocument(
      UUID organizationId,
      UUID establishmentId,
      DocumentServiceArea serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate,
      List<UUID> auditUserIds,
      String fileName,
      String contentType,
      byte[] fileContent,
      CurrentUser currentUser) {
    Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    Establishment establishment =
        establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    User createdByUser = userAccessService.getCurrentUserOrThrow(currentUser);
    List<User> auditUsers = resolveAuditUsers(organizationId, establishmentId, auditUserIds);
    validateDateRange(issueDate, renewalDate);
    validatePdfFile(contentType, fileContent);

    Document document =
        new Document(
            organization,
            establishment,
            createdByUser,
            serviceArea,
            normalizeRequiredText(title),
            normalizeRequiredText(holderName),
            issueDate,
            renewalDate,
            normalizeFileName(fileName),
            PDF_CONTENT_TYPE,
            fileContent.length);
    document.replaceAuditAssignments(auditUsers);

    Document savedDocument = documentRepository.save(document);
    documentFileRepository.save(new DocumentFile(savedDocument.getId(), fileContent));
    return savedDocument;
  }

  /**
   * Updates document metadata and audit assignments.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param serviceArea the document service area
   * @param title the document title
   * @param holderName the document holder name
   * @param issueDate the issue date
   * @param renewalDate the renewal date
   * @param auditUserIds users assigned to acknowledge the document
   * @param currentUser the authenticated user
   * @return the updated document
   */
  @Transactional
  public Document updateDocument(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      DocumentServiceArea serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate,
      List<UUID> auditUserIds,
      CurrentUser currentUser) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    List<User> auditUsers = resolveAuditUsers(organizationId, establishmentId, auditUserIds);
    validateDateRange(issueDate, renewalDate);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    document.setServiceArea(serviceArea);
    document.setTitle(normalizeRequiredText(title));
    document.setHolderName(normalizeRequiredText(holderName));
    document.setIssueDate(issueDate);
    document.setRenewalDate(renewalDate);
    document.replaceAuditAssignments(auditUsers);

    return documentRepository.save(document);
  }

  /**
   * Replaces the stored PDF file for an existing document.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param fileName the uploaded file name
   * @param contentType the uploaded content type
   * @param fileContent the uploaded file content
   * @param currentUser the authenticated user
   * @return the updated document
   */
  @Transactional
  public Document replaceDocumentFile(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      String fileName,
      String contentType,
      byte[] fileContent,
      CurrentUser currentUser) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    validatePdfFile(contentType, fileContent);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    document.setFileName(normalizeFileName(fileName));
    document.setContentType(PDF_CONTENT_TYPE);
    document.setFileSizeBytes(fileContent.length);

    DocumentFile documentFile =
        documentFileRepository
            .findById(documentId)
            .map(
                existingFile -> {
                  existingFile.replaceContent(fileContent);
                  return existingFile;
                })
            .orElseGet(() -> new DocumentFile(documentId, fileContent));

    documentFileRepository.save(documentFile);
    return documentRepository.save(document);
  }

  /**
   * Deletes a document and its stored file.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param currentUser the authenticated user
   */
  @Transactional
  public void deleteDocument(
      UUID organizationId, UUID establishmentId, UUID documentId, CurrentUser currentUser) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    documentFileRepository.findById(documentId).ifPresent(documentFileRepository::delete);
    documentRepository.delete(document);
  }

  /**
   * Records acknowledgement of a document audit assignment for the current user.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param documentId the document identifier
   * @param currentUser the authenticated user
   * @return the updated document
   */
  @Transactional
  public Document acknowledgeDocumentAudit(
      UUID organizationId, UUID establishmentId, UUID documentId, CurrentUser currentUser) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    User actor = userAccessService.getCurrentUserOrThrow(currentUser);
    DocumentAuditAssignment assignment =
        document
            .findAuditAssignment(actor.getId())
            .orElseThrow(
                () ->
                    new ForbiddenException(
                        "document_audit_acknowledgement_forbidden",
                        "You are not assigned to acknowledge this document"));

    assignment.acknowledge(Instant.now(clock));
    return documentRepository.save(document);
  }

  private Document findDocumentOrThrow(UUID organizationId, UUID establishmentId, UUID documentId) {
    return documentRepository
        .findByIdAndEstablishmentIdAndOrganizationId(documentId, establishmentId, organizationId)
        .orElseThrow(
            () -> new ResourceNotFoundException("document_not_found", "Document not found"));
  }

  private void validateDateRange(LocalDate issueDate, LocalDate renewalDate) {
    if (renewalDate.isBefore(issueDate)) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "invalid_document_dates",
          "Renewal date cannot be before issue date");
    }
  }

  private void validatePdfFile(String contentType, byte[] fileContent) {
    if (fileContent == null || fileContent.length == 0) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST, "document_file_required", "A PDF file is required");
    }

    if (contentType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType.strip())) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST, "invalid_document_file_type", "Only PDF files are supported");
    }
  }

  private String normalizeRequiredText(String value) {
    return value.strip();
  }

  private List<User> resolveAuditUsers(
      UUID organizationId, UUID establishmentId, List<UUID> auditUserIds) {
    if (auditUserIds == null || auditUserIds.isEmpty()) {
      return List.of();
    }

    LinkedHashSet<UUID> uniqueUserIds = new LinkedHashSet<>(auditUserIds);
    return uniqueUserIds.stream()
        .map(userId -> getAuditUserOrThrow(organizationId, establishmentId, userId))
        .toList();
  }

  private User getAuditUserOrThrow(UUID organizationId, UUID establishmentId, UUID userId) {
    User user = userAccessService.getUserOrThrow(userId);
    boolean hasActiveMembership =
        organizationMembershipRepository
            .findByOrganizationIdAndUserId(organizationId, userId)
            .filter(OrganizationMembership::isActive)
            .filter(membership -> membership.hasEstablishmentAccess(establishmentId))
            .isPresent();

    if (!user.isActive() || !hasActiveMembership) {
      throw new ForbiddenException(
          "document_audit_user_forbidden",
          "Documents can only be assigned for audit acknowledgement to active members with access to the establishment");
    }

    return user;
  }

  private String normalizeFileName(String fileName) {
    String normalized = fileName == null ? "" : fileName.strip().replace('\\', '/');
    int lastSeparatorIndex = normalized.lastIndexOf('/');
    String baseName =
        lastSeparatorIndex >= 0 ? normalized.substring(lastSeparatorIndex + 1) : normalized;
    return baseName.isBlank() ? "document.pdf" : baseName;
  }
}
