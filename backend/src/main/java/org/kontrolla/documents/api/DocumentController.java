package org.kontrolla.documents.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.documents.application.DocumentService;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping("/establishments/{establishmentId}/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentResponse createDocument(
			@PathVariable UUID organizationId,
			@PathVariable UUID establishmentId,
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateDocumentRequest request
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
						currentUser
				),
				clock
		);
	}

	@PutMapping("/establishments/{establishmentId}/documents/{documentId}")
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
						currentUser
				),
				clock
		);
	}
}
