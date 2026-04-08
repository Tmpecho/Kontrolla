package org.kontrolla.documents.application;

import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.documents.domain.Document;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DocumentService {

  private static final String PDF_CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;

  private final DocumentRepository documentRepository;
  private final DocumentFileRepository documentFileRepository;
  private final OrganizationAccessService organizationAccessService;
  private final EstablishmentService establishmentService;
  private final UserAccessService userAccessService;

  public DocumentService(
      DocumentRepository documentRepository,
      DocumentFileRepository documentFileRepository,
      OrganizationAccessService organizationAccessService,
      EstablishmentService establishmentService,
      UserAccessService userAccessService
  ) {
    this.documentRepository = documentRepository;
    this.documentFileRepository = documentFileRepository;
    this.organizationAccessService = organizationAccessService;
    this.establishmentService = establishmentService;
    this.userAccessService = userAccessService;
  }

  @Transactional(readOnly = true)
  public Page<Document> listDocuments(
      UUID organizationId,
      UUID establishmentId,
      DocumentServiceArea serviceArea,
      CurrentUser currentUser,
      Pageable pageable
  ) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    return documentRepository.findByEstablishmentIdAndOrganizationIdAndServiceArea(
        establishmentId,
        organizationId,
        serviceArea,
        pageable
    );
  }

  @Transactional(readOnly = true)
  public Document getDocument(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      CurrentUser currentUser
  ) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    return findDocumentOrThrow(organizationId, establishmentId, documentId);
  }

  @Transactional(readOnly = true)
  public DocumentFileDownload getDocumentFile(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      CurrentUser currentUser
  ) {
    Document document = getDocument(organizationId, establishmentId, documentId, currentUser);
    DocumentFile documentFile = documentFileRepository.findById(documentId)
        .orElseThrow(() -> new ResourceNotFoundException("document_file_not_found", "Document file not found"));

    return new DocumentFileDownload(
        document.getFileName(),
        document.getContentType(),
        document.getFileSizeBytes(),
        documentFile.getContent()
    );
  }

  @Transactional
  public Document createDocument(
      UUID organizationId,
      UUID establishmentId,
      DocumentServiceArea serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate,
      String fileName,
      String contentType,
      byte[] fileContent,
      CurrentUser currentUser
  ) {
    Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    User createdByUser = userAccessService.getCurrentUserOrThrow(currentUser);
    validateDateRange(issueDate, renewalDate);
    validatePdfFile(contentType, fileContent);

    Document document = new Document(
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
        fileContent.length
    );

    Document savedDocument = documentRepository.save(document);
    documentFileRepository.save(new DocumentFile(savedDocument.getId(), fileContent));
    return savedDocument;
  }

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
      CurrentUser currentUser
  ) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    validateDateRange(issueDate, renewalDate);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    document.setServiceArea(serviceArea);
    document.setTitle(normalizeRequiredText(title));
    document.setHolderName(normalizeRequiredText(holderName));
    document.setIssueDate(issueDate);
    document.setRenewalDate(renewalDate);

    return documentRepository.save(document);
  }

  @Transactional
  public Document replaceDocumentFile(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      String fileName,
      String contentType,
      byte[] fileContent,
      CurrentUser currentUser
  ) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    validatePdfFile(contentType, fileContent);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    document.setFileName(normalizeFileName(fileName));
    document.setContentType(PDF_CONTENT_TYPE);
    document.setFileSizeBytes(fileContent.length);

    DocumentFile documentFile = documentFileRepository.findById(documentId)
        .map(existingFile -> {
          existingFile.replaceContent(fileContent);
          return existingFile;
        })
        .orElseGet(() -> new DocumentFile(documentId, fileContent));

    documentFileRepository.save(documentFile);
    return documentRepository.save(document);
  }

  @Transactional
  public void deleteDocument(
      UUID organizationId,
      UUID establishmentId,
      UUID documentId,
      CurrentUser currentUser
  ) {
    organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

    Document document = findDocumentOrThrow(organizationId, establishmentId, documentId);
    documentFileRepository.findById(documentId).ifPresent(documentFileRepository::delete);
    documentRepository.delete(document);
  }

  private Document findDocumentOrThrow(UUID organizationId, UUID establishmentId, UUID documentId) {
    return documentRepository.findByIdAndEstablishmentIdAndOrganizationId(documentId, establishmentId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("document_not_found", "Document not found"));
  }

  private void validateDateRange(LocalDate issueDate, LocalDate renewalDate) {
    if (renewalDate.isBefore(issueDate)) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "invalid_document_dates",
          "Renewal date cannot be before issue date"
      );
    }
  }

  private void validatePdfFile(String contentType, byte[] fileContent) {
    if (fileContent == null || fileContent.length == 0) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "document_file_required",
          "A PDF file is required"
      );
    }

    if (contentType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType.strip())) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "invalid_document_file_type",
          "Only PDF files are supported"
      );
    }
  }

  private String normalizeRequiredText(String value) {
    return value.strip();
  }

  private String normalizeFileName(String fileName) {
    String normalized = fileName == null ? "" : fileName.strip().replace('\\', '/');
    int lastSeparatorIndex = normalized.lastIndexOf('/');
    String baseName = lastSeparatorIndex >= 0 ? normalized.substring(lastSeparatorIndex + 1) : normalized;
    return baseName.isBlank() ? "document.pdf" : baseName;
  }
}
