package org.kontrolla.deviations.application;

import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationEvent;
import org.kontrolla.deviations.domain.DeviationEventType;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		return deviationRepository.findByEstablishmentIdAndOrganizationId(establishmentId, organizationId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Deviation> listDeviationsByOrganizationId(
			UUID organizationId,
			CurrentUser currentUser,
			Pageable pageable
	) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireOrganizationWideOperationalAccess(currentUser, organizationId);
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
		String normalizedTitle = normalizeRequiredText(title);
		String normalizedDescription = normalizeRequiredText(description);

		Deviation deviation = new Deviation(
				organization,
				establishment,
				createdByUser,
				null,
				normalizedTitle,
				normalizedDescription,
				severity,
				category
		);
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.REPORTED,
				createdByUser,
				Instant.now(),
				"Deviation reported."
		));

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

		User actor = userAccessService.getCurrentUserOrThrow(currentUser);
		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		User assignedUser = getAssignableUserOrThrow(organizationId, establishmentId, assignedUserId);

		if (deviation.getAssignedToUser() != null && deviation.getAssignedToUser().getId().equals(assignedUser.getId())) {
			return deviation;
		}

		deviation.setAssignedToUser(assignedUser);
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.ASSIGNED,
				actor,
				Instant.now(),
				"Deviation assigned to " + formatUserDisplayName(assignedUser) + "."
		));

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

		User actor = userAccessService.getCurrentUserOrThrow(currentUser);
		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);

		if (deviation.getStatus() == status) {
			return deviation;
		}

		DeviationStatus previousStatus = deviation.getStatus();
		deviation.setStatus(status);
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.STATUS_CHANGED,
				actor,
				Instant.now(),
				"Status changed from " + formatStatusLabel(previousStatus) + " to " + formatStatusLabel(status) + "."
		));

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

		User actor = userAccessService.getCurrentUserOrThrow(currentUser);
		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		List<String> changedFields = new ArrayList<>();
		String normalizedTitle = normalizeRequiredText(title);
		String normalizedDescription = normalizeRequiredText(description);

		if (!deviation.getTitle().equals(normalizedTitle)) {
			deviation.setTitle(normalizedTitle);
			changedFields.add("title");
		}
		if (!deviation.getDescription().equals(normalizedDescription)) {
			deviation.setDescription(normalizedDescription);
			changedFields.add("description");
		}
		if (deviation.getSeverity() != severity) {
			deviation.setSeverity(severity);
			changedFields.add("severity");
		}
		if (deviation.getCategory() != category) {
			deviation.setCategory(category);
			changedFields.add("category");
		}

		if (changedFields.isEmpty()) {
			return deviation;
		}

		deviation.addEvent(new DeviationEvent(
				DeviationEventType.DETAILS_UPDATED,
				actor,
				Instant.now(),
				buildDetailsUpdatedNote(changedFields)
		));

		return deviationRepository.save(deviation);
	}

	@Transactional
	public Deviation addTimelineNote(
			UUID organizationId,
			UUID establishmentId,
			UUID deviationId,
			String note,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

		User actor = userAccessService.getCurrentUserOrThrow(currentUser);
		Deviation deviation = findDeviationOrThrow(organizationId, establishmentId, deviationId);
		deviation.addEvent(new DeviationEvent(
				DeviationEventType.NOTE_ADDED,
				actor,
				Instant.now(),
				note.strip()
		));

		return deviationRepository.save(deviation);
	}

	private Deviation findDeviationOrThrow(UUID organizationId, UUID establishmentId, UUID deviationId) {
		return deviationRepository.findByIdAndEstablishmentIdAndOrganizationId(deviationId, establishmentId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("deviation_not_found", "Deviation not found"));
	}

	private String normalizeRequiredText(String value) {
		return Objects.requireNonNull(value, "value").strip();
	}

	private User getAssignableUserOrThrow(UUID organizationId, UUID establishmentId, UUID assignedUserId) {
		User user = userAccessService.getUserOrThrow(assignedUserId);
		boolean hasActiveMembership = organizationMembershipRepository.findByOrganizationIdAndUserId(organizationId, assignedUserId)
				.filter(OrganizationMembership::isActive)
				.filter(membership -> membership.hasEstablishmentAccess(establishmentId))
				.isPresent();

		if (!user.isActive() || !hasActiveMembership) {
			throw new ForbiddenException(
					"deviation_assignment_user_forbidden",
					"Deviations can only be assigned to active members with access to the establishment"
			);
		}

		return user;
	}

	private String buildDetailsUpdatedNote(List<String> changedFields) {
		if (changedFields.size() == 1) {
			return "Updated " + changedFields.get(0) + ".";
		}

		if (changedFields.size() == 2) {
			return "Updated " + changedFields.get(0) + " and " + changedFields.get(1) + ".";
		}

		StringBuilder note = new StringBuilder("Updated ");
		for (int index = 0; index < changedFields.size(); index++) {
			if (index == changedFields.size() - 1) {
				note.append("and ").append(changedFields.get(index));
			} else {
				note.append(changedFields.get(index)).append(", ");
			}
		}
		note.append('.');
		return note.toString();
	}

	private String formatStatusLabel(DeviationStatus status) {
		return status.name().toLowerCase().replace('_', ' ');
	}

	private String formatUserDisplayName(User user) {
		String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
		return fullName.isBlank() ? user.getEmail() : fullName;
	}
}
