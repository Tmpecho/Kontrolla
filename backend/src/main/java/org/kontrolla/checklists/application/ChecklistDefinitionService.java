package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.domain.ChecklistTaskDefinition;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistDefinitionService {

	private static final String DEFAULT_TIMEZONE = "Europe/Oslo";
	private static final long GENERATION_LOOKBACK_DAYS = 1;
	private static final long GENERATION_LOOKAHEAD_DAYS = 60;

	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final ChecklistRunService checklistRunService;
	private final ChecklistSchedulerService checklistSchedulerService;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final UserRepository userRepository;

	public ChecklistDefinitionService(
			ChecklistDefinitionRepository checklistDefinitionRepository,
			ChecklistRunService checklistRunService,
			ChecklistSchedulerService checklistSchedulerService,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserRepository userRepository
	) {
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.checklistRunService = checklistRunService;
		this.checklistSchedulerService = checklistSchedulerService;
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
			CreateChecklistDefinitionCommand command,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		User actor = getUserOrThrow(currentUser.userId());
		Instant now = Instant.now();

		ChecklistDefinition checklistDefinition = new ChecklistDefinition(
				UUID.randomUUID(),
				establishment,
				command.serviceArea(),
				command.title(),
				command.description(),
				1,
				resolveDefinitionStatusForNewVersion(null),
				now,
				actor,
				actor
		);
		checklistDefinition.replaceTasks(toChecklistTasks(command.tasks()));
		checklistDefinition.replaceSchedules(toChecklistSchedules(command.schedules(), actor));

		ChecklistDefinition savedDefinition = checklistDefinitionRepository.save(checklistDefinition);
		generateScheduledRuns(establishment, actor, now, savedDefinition.getStatus());
		return savedDefinition;
	}

	@Transactional
	public ChecklistDefinition createChecklistDefinition(
			UUID organizationId,
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			String title,
			String description,
			List<ChecklistTaskInput> tasks,
			List<ChecklistScheduleInput> schedules,
			CurrentUser currentUser
	) {
		return createChecklistDefinition(
				organizationId,
				establishmentId,
				new CreateChecklistDefinitionCommand(
						serviceArea,
						title,
						description,
						toChecklistDefinitionTaskInputs(tasks),
						toChecklistDefinitionScheduleInputs(schedules)
				),
				currentUser
		);
	}

	@Transactional
	public ChecklistDefinition updateChecklistDefinition(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistDefinitionId,
			UpdateChecklistDefinitionCommand command,
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
		checklistRunService.cancelRegeneratableRunsForDefinitionGroup(
				establishmentId,
				currentDefinition.getDefinitionGroupId(),
				now.minus(GENERATION_LOOKBACK_DAYS, ChronoUnit.DAYS),
				actor.getId(),
				"definition_updated"
		);

		currentDefinition.supersede(now, actor);

		ChecklistDefinition nextDefinition = new ChecklistDefinition(
				currentDefinition.getDefinitionGroupId(),
				currentDefinition.getEstablishment(),
				command.serviceArea(),
				command.title(),
				command.description(),
				currentDefinition.getVersionNumber() + 1,
				resolveDefinitionStatusForNewVersion(command.status()),
				now,
				actor,
				actor
		);
		nextDefinition.replaceTasks(toChecklistTasks(command.tasks()));
		nextDefinition.replaceSchedules(toChecklistSchedules(command.schedules(), actor));

		ChecklistDefinition savedDefinition = checklistDefinitionRepository.save(nextDefinition);
		generateScheduledRuns(currentDefinition.getEstablishment(), actor, now, savedDefinition.getStatus());
		return savedDefinition;
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
			List<ChecklistTaskInput> tasks,
			List<ChecklistScheduleInput> schedules,
			CurrentUser currentUser
	) {
		return updateChecklistDefinition(
				organizationId,
				establishmentId,
				checklistDefinitionId,
				new UpdateChecklistDefinitionCommand(
						serviceArea,
						title,
						description,
						status,
						toChecklistDefinitionTaskInputs(tasks),
						toChecklistDefinitionScheduleInputs(schedules)
				),
				currentUser
		);
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

	private List<ChecklistTaskDefinition> toChecklistTasks(List<ChecklistDefinitionTaskInput> tasks) {
		return tasks.stream()
				.map(task -> new ChecklistTaskDefinition(
						task.title(),
						task.details(),
						task.taskKind(),
						task.required(),
						task.sortOrder(),
						task.measurementUnit(),
						task.minimumAllowedValue(),
						task.maximumAllowedValue()
				))
				.toList();
	}

	private List<ChecklistDefinitionTaskInput> toChecklistDefinitionTaskInputs(List<ChecklistTaskInput> tasks) {
		return tasks.stream()
				.map(task -> new ChecklistDefinitionTaskInput(
						task.title(),
						task.details(),
						task.taskKind(),
						task.required(),
						task.sortOrder(),
						task.measurementUnit(),
						task.minimumAllowedValue(),
						task.maximumAllowedValue()
				))
				.toList();
	}

	private List<ChecklistSchedule> toChecklistSchedules(List<ChecklistDefinitionScheduleInput> schedules, User actor) {
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

	private List<ChecklistDefinitionScheduleInput> toChecklistDefinitionScheduleInputs(List<ChecklistScheduleInput> schedules) {
		if (schedules == null) {
			return List.of();
		}

		return schedules.stream()
				.map(schedule -> new ChecklistDefinitionScheduleInput(
						schedule.scheduleType(),
						schedule.startDate(),
						schedule.endDate(),
						schedule.dueTime(),
						schedule.weekdayMask(),
						schedule.dayOfMonth(),
						schedule.timezone(),
						schedule.active()
				))
				.toList();
	}

	private User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));
	}

	private void generateScheduledRuns(
			Establishment establishment,
			User actor,
			Instant now,
			ChecklistDefinitionStatus status
	) {
		if (status != ChecklistDefinitionStatus.ACTIVE) {
			return;
		}

		checklistSchedulerService.generateRunsForWindowInternal(
				establishment,
				now.minus(GENERATION_LOOKBACK_DAYS, ChronoUnit.DAYS),
				now.plus(GENERATION_LOOKAHEAD_DAYS, ChronoUnit.DAYS),
				actor.getId()
		);
		checklistRunService.markOverdueRuns(
				establishment.getOrganization().getId(),
				establishment.getId(),
				now,
				actor.getId()
		);
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

	public record ChecklistTaskInput(
			String title,
			String details,
			ChecklistTaskKind taskKind,
			boolean required,
			int sortOrder,
			String measurementUnit,
			BigDecimal minimumAllowedValue,
			BigDecimal maximumAllowedValue
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
