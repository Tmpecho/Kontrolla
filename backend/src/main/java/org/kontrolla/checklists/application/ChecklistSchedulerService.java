package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistSchedulerService {

	private final ChecklistDefinitionRepository checklistDefinitionRepository;
	private final ChecklistRunRepository checklistRunRepository;
	private final ChecklistRunService checklistRunService;
	private final OrganizationAccessService organizationAccessService;
	private final EstablishmentService establishmentService;
	private final UserRepository userRepository;

	public ChecklistSchedulerService(
			ChecklistDefinitionRepository checklistDefinitionRepository,
			ChecklistRunRepository checklistRunRepository,
			ChecklistRunService checklistRunService,
			OrganizationAccessService organizationAccessService,
			EstablishmentService establishmentService,
			UserRepository userRepository
	) {
		this.checklistDefinitionRepository = checklistDefinitionRepository;
		this.checklistRunRepository = checklistRunRepository;
		this.checklistRunService = checklistRunService;
		this.organizationAccessService = organizationAccessService;
		this.establishmentService = establishmentService;
		this.userRepository = userRepository;
	}

	@Transactional
	public int generateRunsForWindow(
			UUID organizationId,
			UUID establishmentId,
			Instant windowStart,
			Instant windowEnd,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		User actor = getUserOrThrow(currentUser.userId());

		List<ChecklistDefinition> activeDefinitions = checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				establishmentId,
				ChecklistDefinitionStatus.ACTIVE
		);

		int createdRuns = 0;
		for (ChecklistDefinition definition : activeDefinitions) {
			for (ChecklistSchedule schedule : definition.getSchedules()) {
				if (!schedule.isActive()) {
					continue;
				}

				for (Instant dueAt : calculateDueInstants(schedule, windowStart, windowEnd)) {
					if (checklistRunRepository.existsByChecklistDefinitionIdAndDueAt(definition.getId(), dueAt)) {
						continue;
					}

					ChecklistRun checklistRun = new ChecklistRun(
							definition,
							definition.getDefinitionGroupId(),
							establishment,
							definition.getServiceArea(),
							definition.getTitle(),
							definition.getDescription(),
							dueAt,
							ChecklistRunStatus.PENDING,
							actor
					);
					checklistRun.snapshotItemsFromDefinition(definition.getItems());
					checklistRun.addEvent(new ChecklistRunEvent(
							ChecklistRunEventType.CREATED,
							actor,
							Instant.now(),
							null
					));
					checklistRunRepository.save(checklistRun);
					createdRuns++;
				}
			}
		}

		return createdRuns;
	}

	@Transactional
	public int markOverdueRuns(
			UUID organizationId,
			UUID establishmentId,
			Instant now,
			CurrentUser currentUser
	) {
		organizationAccessService.requireEstablishmentManagement(currentUser, organizationId);
		establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
		return checklistRunService.markOverdueRuns(establishmentId, now);
	}

	private List<Instant> calculateDueInstants(ChecklistSchedule schedule, Instant windowStart, Instant windowEnd) {
		ZoneId zoneId = ZoneId.of(schedule.getTimezone());
		LocalDate startDate = windowStart.atZone(zoneId).toLocalDate();
		LocalDate endDate = windowEnd.atZone(zoneId).toLocalDate();
		LocalDate scheduleStart = schedule.getStartDate();
		LocalDate scheduleEnd = schedule.getEndDate();
		LocalDate effectiveStart = startDate.isAfter(scheduleStart) ? startDate : scheduleStart;
		LocalDate effectiveEnd = scheduleEnd == null || endDate.isBefore(scheduleEnd) ? endDate : scheduleEnd;

		if (effectiveStart.isAfter(effectiveEnd)) {
			return List.of();
		}

		List<Instant> dueInstants = new ArrayList<>();
		LocalDate currentDate = effectiveStart;
		while (!currentDate.isAfter(effectiveEnd)) {
			if (matchesSchedule(schedule, currentDate)) {
				Instant dueAt = toDueInstant(schedule, currentDate, zoneId);
				if (!dueAt.isBefore(windowStart) && !dueAt.isAfter(windowEnd)) {
					dueInstants.add(dueAt);
				}
			}
			currentDate = currentDate.plusDays(1);
		}
		return dueInstants;
	}

	private boolean matchesSchedule(ChecklistSchedule schedule, LocalDate date) {
		return switch (schedule.getScheduleType()) {
			case ONE_OFF -> date.equals(schedule.getStartDate());
			case DAILY -> true;
			case WEEKLY -> schedule.getWeekdayMask() != null && (schedule.getWeekdayMask() & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
			case MONTHLY -> schedule.getDayOfMonth() != null && date.getDayOfMonth() == normalizeDayOfMonth(schedule.getDayOfMonth(), date);
		};
	}

	private int normalizeDayOfMonth(int configuredDayOfMonth, LocalDate date) {
		int lastDayOfMonth = YearMonth.from(date).lengthOfMonth();
		return Math.min(configuredDayOfMonth, lastDayOfMonth);
	}

	private Instant toDueInstant(ChecklistSchedule schedule, LocalDate date, ZoneId zoneId) {
		LocalDateTime dueDateTime = LocalDateTime.of(
				date,
				schedule.getDueTime() == null ? java.time.LocalTime.MIDNIGHT : schedule.getDueTime()
		);
		return dueDateTime.atZone(zoneId).toInstant();
	}

	private User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new org.kontrolla.common.exception.ResourceNotFoundException("user_not_found", "User not found"));
	}
}
