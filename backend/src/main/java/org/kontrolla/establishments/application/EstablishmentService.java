package org.kontrolla.establishments.application;

import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentServingHours;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentServingHoursRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * Handles establishment access, lookup, creation, and serving-hours management.
 */
@Service
public class EstablishmentService {

	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentServingHoursRepository servingHoursRepository;
	private final OrganizationAccessService organizationAccessService;

	/**
	 * Creates an establishment service backed by organization and persistence
	 * services.
	 *
	 * @param establishmentRepository repository for establishments
	 * @param servingHoursRepository repository for serving-hours records
	 * @param organizationAccessService service for organization access checks
	 */
	public EstablishmentService(
			EstablishmentRepository establishmentRepository,
			EstablishmentServingHoursRepository servingHoursRepository,
			OrganizationAccessService organizationAccessService
	) {
		this.establishmentRepository = establishmentRepository;
		this.servingHoursRepository = servingHoursRepository;
		this.organizationAccessService = organizationAccessService;
	}

	/**
	 * Lists establishments visible to the current user within an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param currentUser the authenticated user
	 * @param pageable pagination information
	 * @return a page of accessible establishments
	 */
	@Transactional(readOnly = true)
	public Page<Establishment> listEstablishments(UUID organizationId, CurrentUser currentUser, Pageable pageable) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		if (currentUser.isPlatformAdmin()) {
			return establishmentRepository.findByOrganizationId(organizationId, pageable);
		}

		var membership = organizationAccessService.getActiveMembershipOrThrow(currentUser, organizationId);
		if (membership.isAccessAllEstablishments() || membership.getRole() == org.kontrolla.organizations.domain.OrganizationRole.ORG_OWNER || membership.getRole() == org.kontrolla.organizations.domain.OrganizationRole.ORG_ADMIN) {
			return establishmentRepository.findByOrganizationId(organizationId, pageable);
		}

		return establishmentRepository.findAccessibleByOrganizationIdAndUserId(organizationId, currentUser.userId(), pageable);
	}

	/**
	 * Returns a single establishment after verifying organization and
	 * establishment access.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @return the matching establishment
	 */
	@Transactional(readOnly = true)
	public Establishment getEstablishment(UUID organizationId, UUID establishmentId, CurrentUser currentUser) {
		organizationAccessService.getOrganizationOrThrow(organizationId);
		organizationAccessService.requireEstablishmentAccess(currentUser, organizationId, establishmentId);
		return establishmentRepository.findByIdAndOrganizationId(establishmentId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("establishment_not_found", "Establishment not found"));
	}

	/**
	 * Creates a new establishment within an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param name the establishment name
	 * @param type the establishment type
	 * @param status the establishment status
	 * @param currentUser the authenticated user
	 * @return the created establishment
	 */
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

	/**
	 * Returns serving hours for every weekday for an establishment, defaulting
	 * missing days to closed.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param currentUser the authenticated user
	 * @return serving hours for all weekdays
	 */
	@Transactional(readOnly = true)
	public java.util.List<ServingHoursDayView> getServingHours(
			UUID organizationId,
			UUID establishmentId,
			CurrentUser currentUser
	) {
		getEstablishment(organizationId, establishmentId, currentUser);
		var configuredHours = servingHoursRepository.findByEstablishmentIdOrderByDayOfWeekAsc(establishmentId)
				.stream()
				.collect(java.util.stream.Collectors.toMap(
						EstablishmentServingHours::getDayOfWeek,
						ServingHoursDayView::from
				));

		return java.util.Arrays.stream(DayOfWeek.values())
				.map(dayOfWeek -> configuredHours.getOrDefault(dayOfWeek, ServingHoursDayView.closed(dayOfWeek)))
				.toList();
	}

	/**
	 * Replaces serving hours for an establishment across all weekdays.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param commands the weekday updates to apply
	 * @param currentUser the authenticated user
	 * @return the updated serving hours for all weekdays
	 */
	@Transactional
	public java.util.List<ServingHoursDayView> updateServingHours(
			UUID organizationId,
			UUID establishmentId,
			java.util.List<UpdateServingHoursDayCommand> commands,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = getEstablishment(organizationId, establishmentId, currentUser);
		validateServingHours(commands);

		var existingByDay = servingHoursRepository.findByEstablishmentIdOrderByDayOfWeekAsc(establishmentId)
				.stream()
				.collect(java.util.stream.Collectors.toMap(
						EstablishmentServingHours::getDayOfWeek,
						hours -> hours
				));

		for (UpdateServingHoursDayCommand command : commands) {
			EstablishmentServingHours hours = existingByDay.get(command.dayOfWeek());
			if (hours == null) {
				hours = new EstablishmentServingHours(
						establishment,
						command.dayOfWeek(),
						command.closed(),
						command.opensAt(),
						command.closesAt()
				);
			} else {
				hours.update(command.closed(), command.opensAt(), command.closesAt());
			}
			servingHoursRepository.save(hours);
		}

		return getServingHours(organizationId, establishmentId, currentUser);
	}

	private void validateServingHours(java.util.List<UpdateServingHoursDayCommand> commands) {
		if (commands == null || commands.size() != DayOfWeek.values().length) {
			throw new ApplicationException(
					HttpStatus.BAD_REQUEST,
					"invalid_serving_hours",
					"Serving hours must include exactly one entry for each day of the week"
			);
		}

		var commandsByDay = new java.util.EnumMap<DayOfWeek, UpdateServingHoursDayCommand>(DayOfWeek.class);
		for (UpdateServingHoursDayCommand command : commands) {
			if (commandsByDay.put(command.dayOfWeek(), command) != null) {
				throw new ApplicationException(
						HttpStatus.BAD_REQUEST,
						"invalid_serving_hours",
						"Serving hours must not contain duplicate days"
				);
			}

			if (command.closed()) {
				if (command.opensAt() != null || command.closesAt() != null) {
					throw new ApplicationException(
							HttpStatus.BAD_REQUEST,
							"invalid_serving_hours",
							"Closed days cannot include opening or closing times"
					);
				}
				continue;
			}

			if (command.opensAt() == null || command.closesAt() == null) {
				throw new ApplicationException(
						HttpStatus.BAD_REQUEST,
						"invalid_serving_hours",
						"Open days must include both opening and closing times"
				);
			}

			if (command.opensAt().equals(command.closesAt())) {
				throw new ApplicationException(
						HttpStatus.BAD_REQUEST,
						"invalid_serving_hours",
						"Opening and closing times must differ"
				);
			}
		}
	}
}
