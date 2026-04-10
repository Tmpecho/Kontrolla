package org.kontrolla.documents.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.documents.application.DocumentService;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class DocumentController {

  private final DocumentService documentService;
  private final Clock clock;

  public DocumentController(DocumentService documentService, Clock clock) {
    this.documentService = documentService;
    this.clock = clock;
  }

  @GetMapping("/establishments/{establishmentId}/documents")
  public PageResponse<DocumentResponse> listDocuments(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @RequestParam DocumentServiceArea serviceArea,
      @AuthenticationPrincipal CurrentUser currentUser,
      @PageableDefault(size = 20, sort = "renewalDate", direction = Sort.Direction.ASC) Pageable pageable
  ) {
    return PageResponse.from(
        documentService.listDocuments(organizationId, establishmentId, serviceArea, currentUser, pageable),
        document -> DocumentResponse.from(document, clock)
    );
  }

  @GetMapping("/establishments/{establishmentId}/documents/{documentId}")
  public DocumentResponse getDocument(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    return DocumentResponse.from(
        documentService.getDocument(organizationId, establishmentId, documentId, currentUser),
        clock
    );
  }

  @GetMapping("/establishments/{establishmentId}/documents/{documentId}/file")
  public ResponseEntity<byte[]> downloadDocumentFile(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    var file = documentService.getDocumentFile(organizationId, establishmentId, documentId, currentUser);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .contentLength(file.fileSizeBytes())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.fileName()).build().toString()
        )
        .body(file.content());
  }

  @DeleteMapping("/establishments/{establishmentId}/documents/{documentId}")
  public ResponseEntity<Void> deleteDocument(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    documentService.deleteDocument(organizationId, establishmentId, documentId, currentUser);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/establishments/{establishmentId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DocumentResponse createDocument(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestPart("metadata") CreateDocumentRequest request,
      @RequestPart("file") MultipartFile file
  ) {
    return DocumentResponse.from(
        documentService.createDocument(
            organizationId,
            establishmentId,
            request.serviceArea(),
            request.title(),
            request.holderName(),
            request.issueDate(),
            request.renewalDate(),
            request.auditUserIds(),
            file.getOriginalFilename(),
            file.getContentType(),
            getBytes(file),
            currentUser
        ),
        clock
    );
  }

  @PutMapping(value = "/establishments/{establishmentId}/documents/{documentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DocumentResponse updateDocument(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody UpdateDocumentRequest request
  ) {
    return DocumentResponse.from(
        documentService.updateDocument(
            organizationId,
            establishmentId,
            documentId,
            request.serviceArea(),
            request.title(),
            request.holderName(),
            request.issueDate(),
            request.renewalDate(),
            request.auditUserIds(),
            currentUser
        ),
        clock
    );
  }

  @PutMapping(value = "/establishments/{establishmentId}/documents/{documentId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentResponse replaceDocumentFile(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @RequestPart("file") MultipartFile file
  ) {
    return DocumentResponse.from(
        documentService.replaceDocumentFile(
            organizationId,
            establishmentId,
            documentId,
            file.getOriginalFilename(),
            file.getContentType(),
            getBytes(file),
            currentUser
        ),
        clock
    );
  }

  @PostMapping("/establishments/{establishmentId}/documents/{documentId}/acknowledge-read")
  public DocumentResponse acknowledgeDocumentAudit(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID documentId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    return DocumentResponse.from(
        documentService.acknowledgeDocumentAudit(
            organizationId,
            establishmentId,
            documentId,
            currentUser
        ),
        clock
    );
  }

  private byte[] getBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (java.io.IOException exception) {
      throw new org.kontrolla.common.exception.ApplicationException(
          HttpStatus.BAD_REQUEST,
          "document_file_unreadable",
          "Could not read uploaded file"
      );
    }
  }
}
