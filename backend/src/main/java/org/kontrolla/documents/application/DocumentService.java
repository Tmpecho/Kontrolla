package org.kontrolla.documents.application;

import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final UserAccessService userAccessService;

	public DocumentService(
			DocumentRepository documentRepository,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserAccessService userAccessService
	) {
		this.documentRepository = documentRepository;
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

	@Transactional
	public Document createDocument(
			UUID organizationId,
			UUID establishmentId,
			DocumentServiceArea serviceArea,
			String title,
			String holderName,
			LocalDate issueDate,
			LocalDate renewalDate,
			CurrentUser currentUser
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		User createdByUser = userAccessService.getCurrentUserOrThrow(currentUser);
		validateDateRange(issueDate, renewalDate);

		Document document = new Document(
				organization,
				establishment,
				createdByUser,
				serviceArea,
				normalizeRequiredText(title),
				normalizeRequiredText(holderName),
				issueDate,
				renewalDate
		);

		return documentRepository.save(document);
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

	private String normalizeRequiredText(String value) {
		return value.strip();
	}
}
