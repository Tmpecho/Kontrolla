package org.kontrolla.deviations.application;

import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.application.UserAccessService;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.domain.Organization;
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
	private final UserAccessService userAccessService;
	private final OrganizationMembershipRepository organizationMembershipRepository;

	public DeviationService(
			DeviationRepository deviationRepository,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserAccessService userAccessService,
			OrganizationMembershipRepository organizationMembershipRepository
	) {
		this.deviationRepository = deviationRepository;
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.userAccessService = userAccessService;
		this.organizationMembershipRepository = organizationMembershipRepository;
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
		return deviationRepository.findByEstablishmentIdAndOrganizationId(establishmentId, organizationId, pageable);
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

	@Transactional
	public Deviation createDeviation(
			CurrentUser currentUser,
			String title,
			String description,
			DeviationCategory category,
			DeviationSeverity severity,
			UUID organizationId,
			UUID establishmentId
	) {
		Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		User createdByUser = userAccessService.getCurrentUserOrThrow(currentUser);

		Deviation deviation = new Deviation(
				organization,
				establishment,
				createdByUser,
				null,
				title,
				description,
				severity,
				category
		);

		return deviationRepository.save(deviation);
	}

	@Transactional(readOnly = true)
	public Deviation getDeviation(
			UUID organizationId,
			UUID establishmentId,
			UUID deviationId,
			CurrentUser currentUser
	) {
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		return findDeviationOrThrow(organizationId, establishmentId, deviationId);
	}

	@Transactional
	public Deviation assignDeviation(
			UUID organizationId,
			UUID establishmentId,
			UUID deviationId,
			UUID assignedUserId,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		deviation.setAssignedToUser(getAssignableUserOrThrow(organizationId, assignedUserId));

		return deviationRepository.save(deviation);
	}

	@Transactional
	public Deviation updateDeviationStatus(
			UUID organizationId,
			UUID establishmentId,
			UUID deviationId,
			DeviationStatus status,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		deviation.setStatus(status);

		return deviationRepository.save(deviation);
	}

	@Transactional
	public Deviation updateDeviationDetails(
			UUID organizationId,
			UUID establishmentId,
			UUID deviationId,
			String title,
			String description,
			DeviationSeverity severity,
			DeviationCategory category,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		deviation.setTitle(title);
		deviation.setDescription(description);
		deviation.setSeverity(severity);
		deviation.setCategory(category);

		return deviationRepository.save(deviation);
	}

	private Deviation findDeviationOrThrow(UUID organizationId, UUID establishmentId, UUID deviationId) {
		return deviationRepository.findByIdAndEstablishmentIdAndOrganizationId(deviationId, establishmentId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("deviation_not_found", "Deviation not found"));
	}

	private User getAssignableUserOrThrow(UUID organizationId, UUID assignedUserId) {
		User user = userAccessService.getUserOrThrow(assignedUserId);
		boolean hasActiveMembership = organizationMembershipRepository.findByOrganizationIdAndUserId(organizationId, assignedUserId)
				.map(OrganizationMembership::isActive)
				.orElse(false);

		if (!user.isActive() || !hasActiveMembership) {
			throw new ForbiddenException(
					"deviation_assignment_user_forbidden",
					"Deviations can only be assigned to active members of the organization"
			);
		}

		return user;
	}
}
