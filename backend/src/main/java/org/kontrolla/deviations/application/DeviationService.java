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
import org.kontrolla.notifications.application.CreateNotificationCommand;
import org.kontrolla.notifications.application.NotificationService;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationServiceArea;
import org.kontrolla.notifications.domain.NotificationType;
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

/**
 * Handles deviation reporting, assignment, status changes, and timeline
 * updates.
 */
@Service
public class DeviationService {

	private final DeviationRepository deviationRepository;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final UserAccessService userAccessService;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final NotificationService notificationService;

	/**
	 * Creates a deviation service backed by access, persistence, and
	 * notification services.
	 *
	 * @param deviationRepository repository for deviations
	 * @param organizationAccessService service for organization access checks
	 * @param establishmentService service for establishment access and lookup
	 * @param userAccessService service for resolving users
	 * @param organizationMembershipRepository repository for membership lookups
	 * @param notificationService service for sending deviation notifications
	 */
	public DeviationService(
			DeviationRepository deviationRepository,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserAccessService userAccessService,
			OrganizationMembershipRepository organizationMembershipRepository,
			NotificationService notificationService
	) {
		this.deviationRepository = deviationRepository;
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.userAccessService = userAccessService;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.notificationService = notificationService;
	}

	/**
	 * Lists deviations for a single establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @param pageable pagination information
	 * @return a page of deviations
	 */
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

	/**
	 * Lists deviations across an organization for users with organization-wide
	 * operational access.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param pageable pagination information
	 * @return a page of deviations
	 */
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

	/**
	 * Creates a new deviation for an establishment.
	 *
	 * @param currentUser the authenticated user
	 * @param title the deviation title
	 * @param description the deviation description
	 * @param category the deviation category
	 * @param severity the deviation severity
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @return the created deviation
	 */
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

	/**
	 * Returns a single deviation by id after access validation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param currentUser the authenticated user
	 * @return the requested deviation
	 */
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

	/**
	 * Assigns a deviation to a user with access to the establishment.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param assignedUserId the user to assign
	 * @param currentUser the authenticated user
	 * @return the updated deviation
	 */
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
		notificationService.createNotification(new CreateNotificationCommand(
				assignedUser.getId(),
				actor.getId(),
				organizationId,
				establishmentId,
				toServiceArea(deviation.getCategory()),
				NotificationType.DEVIATION_ASSIGNED,
				deviation.getTitle(),
				"You were assigned this deviation.",
				NotificationResourceType.DEVIATION,
				deviation.getId()
		));

		return deviationRepository.save(deviation);
	}

	/**
	 * Updates the status of a deviation and records a timeline event.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param status the new deviation status
	 * @param currentUser the authenticated user
	 * @return the updated deviation
	 */
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
		createAssignedDeviationNotification(
				deviation,
				actor.getId(),
				organizationId,
				establishmentId,
				NotificationType.DEVIATION_STATUS_CHANGED,
				"Status changed from " + formatStatusLabel(previousStatus) + " to " + formatStatusLabel(status) + "."
		);

		return deviationRepository.save(deviation);
	}

	/**
	 * Updates deviation details and records which fields changed.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param title the new title
	 * @param description the new description
	 * @param severity the new severity
	 * @param category the new category
	 * @param currentUser the authenticated user
	 * @return the updated deviation
	 */
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

	/**
	 * Adds a free-form timeline note to a deviation.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param deviationId the deviation identifier
	 * @param note the note to append
	 * @param currentUser the authenticated user
	 * @return the updated deviation
	 */
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
		createAssignedDeviationNotification(
				deviation,
				actor.getId(),
				organizationId,
				establishmentId,
				NotificationType.DEVIATION_NOTE_ADDED,
				note.strip()
		);

		return deviationRepository.save(deviation);
	}

	private void createAssignedDeviationNotification(
			Deviation deviation,
			UUID actorUserId,
			UUID organizationId,
			UUID establishmentId,
			NotificationType notificationType,
			String message
	) {
		if (deviation.getAssignedToUser() == null) {
			return;
		}

		notificationService.createNotification(new CreateNotificationCommand(
				deviation.getAssignedToUser().getId(),
				actorUserId,
				organizationId,
				establishmentId,
				toServiceArea(deviation.getCategory()),
				notificationType,
				deviation.getTitle(),
				message,
				NotificationResourceType.DEVIATION,
				deviation.getId()
		));
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
			return "Updated " + changedFields.getFirst() + ".";
		}

		if (changedFields.size() == 2) {
			return "Updated " + changedFields.getFirst() + " and " + changedFields.get(1) + ".";
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

	private NotificationServiceArea toServiceArea(DeviationCategory category) {
		return switch (category) {
			case TEMPERATURE, HYGIENE, ALLERGEN, STORAGE -> NotificationServiceArea.IK_MAT;
			case AGE_CONTROL, INAPPROPRIATE_BEHAVIOUR, SERVING_HOURS, DOCUMENTATION_AND_TRAINING -> NotificationServiceArea.IK_ALKOHOL;
		};
	}
}
