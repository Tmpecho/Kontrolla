package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistItemDefinition;
import org.kontrolla.checklists.domain.ChecklistResponseType;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistDefinitionService {

	private static final String DEFAULT_TIMEZONE = "Europe/Oslo";

	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final UserRepository userRepository;

	public ChecklistDefinitionService(
			ChecklistDefinitionRepository checklistDefinitionRepository,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserRepository userRepository
	) {
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<ChecklistDefinition> listChecklistDefinitions(
			UUID organizationId,
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			CurrentUser currentUser,
			Pageable pageable
	) {
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		return checklistDefinitionRepository.findByEstablishmentIdAndServiceAreaAndStatus(
				establishmentId,
				serviceArea,
				ChecklistDefinitionStatus.ACTIVE,
				pageable
		);
	}

	@Transactional(readOnly = true)
	public ChecklistDefinition getChecklistDefinition(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistDefinitionId,
			CurrentUser currentUser
	) {
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		return checklistDefinitionRepository.findByIdAndEstablishmentId(checklistDefinitionId, establishmentId)
				.orElseThrow(() -> new ResourceNotFoundException("checklist_definition_not_found", "Checklist definition not found"));
	}

	@Transactional
	public ChecklistDefinition createChecklistDefinition(
			UUID organizationId,
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			List<ChecklistItemInput> items,
			List<ChecklistScheduleInput> schedules,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		User actor = getUserOrThrow(currentUser.userId());
		Instant now = Instant.now();

		ChecklistDefinition checklistDefinition = new ChecklistDefinition(
				UUID.randomUUID(),
				establishment,
				serviceArea,
				title,
				description,
				1,
				resolveDefinitionStatusForNewVersion(null),
				now,
				actor,
				actor
		);
		checklistDefinition.replaceItems(toChecklistItems(items));
		checklistDefinition.replaceSchedules(toChecklistSchedules(schedules, actor));

		return checklistDefinitionRepository.save(checklistDefinition);
	}

	@Transactional
	public ChecklistDefinition updateChecklistDefinition(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistDefinitionId,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			ChecklistDefinitionStatus status,
			List<ChecklistItemInput> items,
			List<ChecklistScheduleInput> schedules,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		ChecklistDefinition currentDefinition = getChecklistDefinition(
				organizationId,
				establishmentId,
				checklistDefinitionId,
				currentUser
		);
		User actor = getUserOrThrow(currentUser.userId());
		Instant now = Instant.now();

		currentDefinition.supersede(now, actor);

		ChecklistDefinition nextDefinition = new ChecklistDefinition(
				currentDefinition.getDefinitionGroupId(),
				currentDefinition.getEstablishment(),
				serviceArea,
				title,
				description,
				currentDefinition.getVersionNumber() + 1,
				resolveDefinitionStatusForNewVersion(status),
				now,
				actor,
				actor
		);
		nextDefinition.replaceItems(toChecklistItems(items));
		nextDefinition.replaceSchedules(toChecklistSchedules(schedules, actor));

		return checklistDefinitionRepository.save(nextDefinition);
	}

	@Transactional(readOnly = true)
	public List<ChecklistDefinition> listChecklistDefinitionVersions(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistDefinitionId,
			CurrentUser currentUser
	) {
		ChecklistDefinition checklistDefinition = getChecklistDefinition(
				organizationId,
				establishmentId,
				checklistDefinitionId,
				currentUser
		);
		return checklistDefinitionRepository.findByDefinitionGroupIdAndEstablishmentIdOrderByVersionNumberAsc(
				checklistDefinition.getDefinitionGroupId(),
				establishmentId
		);
	}

	private List<ChecklistItemDefinition> toChecklistItems(List<ChecklistItemInput> items) {
		return items.stream()
				.map(item -> new ChecklistItemDefinition(
						item.prompt(),
						item.instructionText(),
						item.responseType(),
						item.required(),
						item.sortOrder()
				))
				.toList();
	}

	private List<ChecklistSchedule> toChecklistSchedules(List<ChecklistScheduleInput> schedules, User actor) {
		if (schedules == null) {
			return List.of();
		}

		return schedules.stream()
				.map(schedule -> new ChecklistSchedule(
						schedule.scheduleType(),
						schedule.startDate(),
						schedule.endDate(),
						schedule.dueTime(),
						schedule.weekdayMask(),
						schedule.dayOfMonth(),
						schedule.timezone() == null || schedule.timezone().isBlank() ? DEFAULT_TIMEZONE : schedule.timezone(),
						schedule.active() == null || schedule.active(),
						actor,
						actor
				))
				.toList();
	}

	private User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));
	}

	private ChecklistDefinitionStatus resolveDefinitionStatusForNewVersion(ChecklistDefinitionStatus status) {
		if (status == null) {
			return ChecklistDefinitionStatus.ACTIVE;
		}
		if (status == ChecklistDefinitionStatus.SUPERSEDED) {
			throw new ConflictException(
					"checklist_definition_invalid_status",
					"A new checklist definition version cannot be created in SUPERSEDED state"
			);
		}
		return status;
	}

	public record ChecklistItemInput(
			String prompt,
			String instructionText,
			ChecklistResponseType responseType,
			boolean required,
			int sortOrder
	) {
	}

	public record ChecklistScheduleInput(
			ChecklistScheduleType scheduleType,
			LocalDate startDate,
			LocalDate endDate,
			LocalTime dueTime,
			Integer weekdayMask,
			Integer dayOfMonth,
			String timezone,
			Boolean active
	) {
	}
}
