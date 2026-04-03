package org.kontrolla.deviations.application;

import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

//TODO create EstablishmentAccess class

@Service
public class DeviationService {

	private final DeviationRepository deviationRepository;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;

	public DeviationService(
			DeviationRepository deviationRepository,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService
	) {
		this.deviationRepository = deviationRepository;
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
	}

	@Transactional(readOnly = true)
	public Page<Deviation> listDeviationsByEstablishmentId(
			UUID organizationId,
			UUID establishmentId,
			CurrentUser currentUser,
			Pageable pageable
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return deviationRepository.findByEstablishmentId(establishmentId, organizationId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Deviation> listDeviationsByOrganizationId(
			UUID organizationId,
			CurrentUser currentUser,
			Pageable pageable
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationReadAccess(currentUser, organizationId);
		return deviationRepository.findByOrganizationId(organizationId, pageable);
	}

    /* 
    @Transactional
    public Deviation createDeviation(
        CurrentUser currentUser,
        String title,
        String description,
        DeviationCategory category,
        DeviationSeverity severity,
        UUID organizationId,
        UUID establishmentId,
        UUID createdById
    ) {
        Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
        //Deviation deviation = new Deviation(organization, establishment, )
    }
        */
}
