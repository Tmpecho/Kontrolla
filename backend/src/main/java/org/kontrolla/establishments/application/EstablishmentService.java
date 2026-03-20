package org.kontrolla.establishments.application;

import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EstablishmentService {

	private final EstablishmentRepository establishmentRepository;
	private final OrganizationAccessService organizationAccessService;

	public EstablishmentService(
			EstablishmentRepository establishmentRepository,
			OrganizationAccessService organizationAccessService
	) {
		this.establishmentRepository = establishmentRepository;
		this.organizationAccessService = organizationAccessService;
	}

	@Transactional(readOnly = true)
	public Page<Establishment> listEstablishments(UUID organizationId, CurrentUser currentUser, Pageable pageable) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return establishmentRepository.findByOrganizationId(organizationId, pageable);
	}

	@Transactional(readOnly = true)
	public Establishment getEstablishment(UUID organizationId, UUID establishmentId, CurrentUser currentUser) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return establishmentRepository.findByIdAndOrganizationId(establishmentId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("establishment_not_found", "Establishment not found"));
	}

	@Transactional
	public Establishment createEstablishment(
			UUID organizationId,
			String name,
			EstablishmentType type,
			EstablishmentStatus status,
			CurrentUser currentUser
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = new Establishment(organization, name, type, status);
		return establishmentRepository.save(establishment);
	}
}
