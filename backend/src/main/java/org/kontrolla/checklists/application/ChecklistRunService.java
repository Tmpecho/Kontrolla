package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskExecution;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;
import org.kontrolla.checklists.infrastructure.ChecklistRunAssignmentRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ChecklistRunService {

	private static final List<ChecklistRunStatus> OVERDUE_CANDIDATE_STATUSES = List.of(
			ChecklistRunStatus.PENDING,
			ChecklistRunStatus.IN_PROGRESS
	);

	private final ChecklistRunRepository checklistRunRepository;
	private final ChecklistRunAssignmentRepository checklistRunAssignmentRepository;
	private final ChecklistAccessService checklistAccessService;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final UserRepository userRepository;

	public ChecklistRunService(
			ChecklistRunRepository checklistRunRepository,
			ChecklistRunAssignmentRepository checklistRunAssignmentRepository,
			ChecklistAccessService checklistAccessService,
			OrganizationMembershipRepository organizationMembershipRepository,
			UserRepository userRepository
	) {
		this.checklistRunRepository = checklistRunRepository;
		this.checklistRunAssignmentRepository = checklistRunAssignmentRepository;
		this.checklistAccessService = checklistAccessService;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<ChecklistRun> listChecklistRuns(
			UUID organizationId,
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			List<ChecklistRunStatus> statuses,
			UUID assignedUserId,
			boolean assignedToCurrentUser,
			Instant dueFrom,
			Instant dueTo,
			CurrentUser currentUser,
			Pageable pageable
	) {
		checklistAccessService.requireChecklistReadAccess(organizationId, establishmentId, currentUser);
		UUID effectiveAssignedUserId = resolveAssignedUserFilter(organizationId, assignedUserId, assignedToCurrentUser, currentUser);
		List<ChecklistRunStatus> normalizedStatuses = statuses == null ? List.of() : statuses.stream().filter(Objects::nonNull).distinct().toList();
		Collection<ChecklistRunStatus> statusesForQuery = normalizedStatuses.isEmpty()
				? List.of(ChecklistRunStatus.PENDING)
				: normalizedStatuses;

		return checklistRunRepository.search(
				establishmentId,
				serviceArea,
				statusesForQuery,
				normalizedStatuses.isEmpty(),
				effectiveAssignedUserId,
				dueFrom,
				dueTo,
				pageable
		);
	}

	@Transactional(readOnly = true)
	public ChecklistRun getChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			CurrentUser currentUser
	) {
		checklistAccessService.requireChecklistReadAccess(organizationId, establishmentId, currentUser);
		return findChecklistRunOrThrow(establishmentId, checklistRunId);
	}

	@Transactional
	public ChecklistRun assignChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			List<UUID> assignedUserIds,
			CurrentUser currentUser
	) {
		checklistAccessService.requireChecklistManagementAccess(organizationId, currentUser);
		ChecklistRun checklistRun = findChecklistRunOrThrow(establishmentId, checklistRunId);
		User actor = getUserOrThrow(currentUser.userId());
		Instant now = Instant.now();

		Map<UUID, User> usersById = getAssignableUsersById(organizationId, assignedUserIds);
		Set<UUID> existingAssignments = checklistRun.getAssignments().stream()
				.map(assignment -> assignment.getAssignedUser().getId())
				.collect(LinkedHashSet::new, Set::add, Set::addAll);

		assignedUserIds.stream().filter(assignedUserId -> !existingAssignments.contains(assignedUserId)).forEach(assignedUserId -> {
			User assignedUser = usersById.get(assignedUserId);
			checklistRun.addAssignment(new ChecklistRunAssignment(assignedUser, actor, now));
			checklistRun.addEvent(new ChecklistRunEvent(
					ChecklistRunEventType.ASSIGNED,
					actor,
					now,
					"{\"assignedUserId\":\"%s\"}".formatted(assignedUserId)
			));
			existingAssignments.add(assignedUserId);
		});

		return checklistRunRepository.save(checklistRun);
	}

	@Transactional
	public void removeChecklistRunAssignment(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			UUID assignmentId,
			CurrentUser currentUser
	) {
		checklistAccessService.requireChecklistManagementAccess(organizationId, currentUser);
		findChecklistRunOrThrow(establishmentId, checklistRunId);

		ChecklistRunAssignment assignment = checklistRunAssignmentRepository.findByIdAndChecklistRunId(assignmentId, checklistRunId)
				.orElseThrow(checklistAccessService::checklistAssignmentNotFound);

		checklistRunAssignmentRepository.delete(assignment);
	}

	@Transactional
	public ChecklistRun startChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			CurrentUser currentUser
	) {
		ChecklistRun checklistRun = getChecklistRun(organizationId, establishmentId, checklistRunId, currentUser);
		checklistAccessService.requireChecklistExecutionAccess(organizationId, checklistRun, currentUser);

		if (checklistRun.getStatus() == ChecklistRunStatus.CANCELLED) {
			throw new ConflictException("checklist_run_cancelled", "Cancelled checklist runs cannot be started");
		}
		if (checklistRun.getStatus() == ChecklistRunStatus.COMPLETED) {
			throw new ConflictException("checklist_run_completed", "Completed checklist runs cannot be started");
		}
		if (checklistRun.getStatus() == ChecklistRunStatus.IN_PROGRESS) {
			return checklistRun;
		}

		Instant now = Instant.now();
		if (checklistRun.getStartedAt() == null) {
			checklistRun.setStartedAt(now);
		}
		if (checklistRun.getStatus() == ChecklistRunStatus.PENDING) {
			checklistRun.setStatus(ChecklistRunStatus.IN_PROGRESS);
		}
		checklistRun.addEvent(new ChecklistRunEvent(
				ChecklistRunEventType.STARTED,
				getUserOrThrow(currentUser.userId()),
				now,
				null
		));

		return checklistRunRepository.save(checklistRun);
	}

	@Transactional
	public ChecklistRun submitChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			List<ChecklistTaskExecutionInput> taskExecutions,
			CurrentUser currentUser
	) {
		ChecklistRun checklistRun = getChecklistRun(organizationId, establishmentId, checklistRunId, currentUser);
		checklistAccessService.requireChecklistExecutionAccess(organizationId, checklistRun, currentUser);

		if (checklistRun.getStatus() == ChecklistRunStatus.CANCELLED) {
			throw new ConflictException("checklist_run_cancelled", "Cancelled checklist runs cannot be submitted");
		}
		if (checklistRun.getStatus() == ChecklistRunStatus.COMPLETED) {
			throw new ConflictException("checklist_run_completed", "Checklist run is already completed");
		}

		Map<UUID, ChecklistTaskExecution> taskExecutionsById = checklistRun.getTaskExecutions().stream()
				.collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

		List<ChecklistTaskExecutionInput> normalizedTaskExecutions = normalizeTaskExecutions(taskExecutions);
		User actor = getUserOrThrow(currentUser.userId());
		Instant now = Instant.now();

		normalizedTaskExecutions.forEach(taskExecutionInput -> {
			ChecklistTaskExecution taskExecution = taskExecutionsById.get(taskExecutionInput.checklistTaskExecutionId());
			if (taskExecution == null) {
				throw new ResourceNotFoundException("checklist_task_execution_not_found", "Checklist task execution not found");
			}
			validateTaskExecution(taskExecution, taskExecutionInput);
			applyTaskExecution(taskExecution, taskExecutionInput, actor, now);
		});

		validateRequiredTaskExecutionsCompleted(checklistRun.getTaskExecutions());

		if (checklistRun.getStartedAt() == null) {
			checklistRun.setStartedAt(now);
		}
		checklistRun.setStatus(ChecklistRunStatus.COMPLETED);
		checklistRun.setCompletedAt(now);
		checklistRun.setCompletedByUser(actor);
		checklistRun.addEvent(new ChecklistRunEvent(
				ChecklistRunEventType.COMPLETED,
				actor,
				now,
				null
		));

		return checklistRunRepository.save(checklistRun);
	}

	@Transactional
	public ChecklistRun reopenChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			CurrentUser currentUser
	) {
		checklistAccessService.requireChecklistManagementAccess(organizationId, currentUser);
		ChecklistRun checklistRun = findChecklistRunOrThrow(establishmentId, checklistRunId);

		if (checklistRun.getStatus() != ChecklistRunStatus.COMPLETED && checklistRun.getStatus() != ChecklistRunStatus.CANCELLED) {
			throw new ConflictException("checklist_run_reopen_invalid_state", "Only completed or cancelled runs can be reopened");
		}

		Instant now = Instant.now();
		User actor = getUserOrThrow(currentUser.userId());
		checklistRun.setStatus(ChecklistRunStatus.PENDING);
		checklistRun.setStartedAt(null);
		checklistRun.setCompletedAt(null);
		checklistRun.setCompletedByUser(null);
		checklistRun.addEvent(new ChecklistRunEvent(
				ChecklistRunEventType.REOPENED,
				actor,
				now,
				null
		));

		return checklistRunRepository.save(checklistRun);
	}

	@Transactional
	public ChecklistRun cancelChecklistRun(
			UUID organizationId,
			UUID establishmentId,
			UUID checklistRunId,
			CurrentUser currentUser
	) {
		checklistAccessService.requireChecklistManagementAccess(organizationId, currentUser);
		ChecklistRun checklistRun = findChecklistRunOrThrow(establishmentId, checklistRunId);

		if (checklistRun.getStatus() == ChecklistRunStatus.COMPLETED) {
			throw new ConflictException("checklist_run_completed", "Completed checklist runs cannot be cancelled");
		}
		if (checklistRun.getStatus() == ChecklistRunStatus.CANCELLED) {
			return checklistRun;
		}

		Instant now = Instant.now();
		checklistRun.setStatus(ChecklistRunStatus.CANCELLED);
		checklistRun.addEvent(new ChecklistRunEvent(
				ChecklistRunEventType.CANCELLED,
				getUserOrThrow(currentUser.userId()),
				now,
				null
		));

		return checklistRunRepository.save(checklistRun);
	}

	@Transactional
	public int markOverdueRuns(UUID establishmentId, Instant now) {
		List<ChecklistRun> overdueRuns = checklistRunRepository.findByEstablishmentIdAndStatusInAndDueAtBefore(
				establishmentId,
				OVERDUE_CANDIDATE_STATUSES,
				now
		);

		int updatedRuns = 0;
		for (ChecklistRun checklistRun : overdueRuns) {
			if (checklistRun.getStatus() == ChecklistRunStatus.OVERDUE) {
				continue;
			}
			checklistRun.setStatus(ChecklistRunStatus.OVERDUE);
			updatedRuns++;
		}

		return updatedRuns;
	}

	private UUID resolveAssignedUserFilter(
			UUID organizationId,
			UUID assignedUserId,
			boolean assignedToCurrentUser,
			CurrentUser currentUser
	) {
		if (assignedToCurrentUser) {
			if (assignedUserId != null && !assignedUserId.equals(currentUser.userId())) {
				throw new ConflictException(
						"checklist_run_filter_conflict",
						"assignedToMe cannot be combined with another assignedUserId"
				);
			}
			return currentUser.userId();
		}

		checklistAccessService.requireAssignmentFilterAccess(organizationId, assignedUserId, currentUser);
		return assignedUserId;
	}

	private ChecklistRun findChecklistRunOrThrow(UUID establishmentId, UUID checklistRunId) {
		return checklistRunRepository.findByIdAndEstablishmentId(checklistRunId, establishmentId)
				.orElseThrow(checklistAccessService::checklistRunNotFound);
	}

	private Map<UUID, User> getAssignableUsersById(UUID organizationId, Collection<UUID> userIds) {
		List<User> users = userRepository.findAllById(userIds);
		Map<UUID, User> usersById = users.stream()
				.collect(LinkedHashMap::new, (map, user) -> map.put(user.getId(), user), Map::putAll);

		userIds.forEach(userId -> {
			if (!usersById.containsKey(userId)) {
				throw new ResourceNotFoundException("user_not_found", "User not found");
			}
			boolean hasActiveMembership = organizationMembershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
					.map(OrganizationMembership::isActive)
					.orElse(false);
			if (!hasActiveMembership) {
				throw new ForbiddenException(
						"checklist_assignment_user_forbidden",
						"Checklist runs can only be assigned to active members of the organization"
				);
			}
		});

		return usersById;
	}

	private User getUserOrThrow(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("user_not_found", "User not found"));
	}

	private List<ChecklistTaskExecutionInput> normalizeTaskExecutions(List<ChecklistTaskExecutionInput> taskExecutions) {
		Map<UUID, ChecklistTaskExecutionInput> taskExecutionsById = new LinkedHashMap<>();
		taskExecutions.stream().map(this::normalizeTaskExecutionInput).filter(taskExecution ->
				taskExecutionsById.put(taskExecution.checklistTaskExecutionId(), taskExecution) != null).forEach(_ -> {
			throw new ConflictException(
					"checklist_task_execution_duplicate_submission",
					"Checklist task executions may only contain one entry per task"
			);
		});
		return List.copyOf(taskExecutionsById.values());
	}

	private ChecklistTaskExecutionInput normalizeTaskExecutionInput(ChecklistTaskExecutionInput taskExecutionInput) {
		return new ChecklistTaskExecutionInput(
				taskExecutionInput.checklistTaskExecutionId(),
				taskExecutionInput.executionStatus(),
				normalizeOptionalText(taskExecutionInput.comment()),
				taskExecutionInput.verificationResult(),
				taskExecutionInput.measuredValue(),
				normalizeOptionalText(taskExecutionInput.enteredText())
		);
	}

	private String normalizeOptionalText(String value) {
		if (value == null) {
			return null;
		}

		String normalizedValue = value.strip();
		return normalizedValue.isEmpty() ? null : normalizedValue;
	}

	private void applyTaskExecution(
			ChecklistTaskExecution taskExecution,
			ChecklistTaskExecutionInput taskExecutionInput,
			User actor,
			Instant now
	) {
		taskExecution.setExecutionStatus(taskExecutionInput.executionStatus());
		taskExecution.setComment(taskExecutionInput.comment());
		taskExecution.setVerificationResult(taskExecutionInput.verificationResult());
		taskExecution.setMeasuredValue(taskExecutionInput.measuredValue());
		taskExecution.setEnteredText(taskExecutionInput.enteredText());

		if (taskExecutionInput.executionStatus() == ChecklistTaskExecutionStatus.PENDING) {
			taskExecution.setResolvedAt(null);
			taskExecution.setResolvedByUser(null);
			return;
		}

		taskExecution.setResolvedAt(now);
		taskExecution.setResolvedByUser(actor);
	}

	private void validateTaskExecution(ChecklistTaskExecution taskExecution, ChecklistTaskExecutionInput taskExecutionInput) {
		if (taskExecutionInput.executionStatus() == ChecklistTaskExecutionStatus.SKIPPED && taskExecution.isRequired()) {
			throw new ConflictException(
					"checklist_task_execution_required_skip_forbidden",
					"Required checklist tasks cannot be skipped"
			);
		}

		boolean hasVerificationResult = taskExecutionInput.verificationResult() != null;
		boolean hasMeasuredValue = taskExecutionInput.measuredValue() != null;
		boolean hasEnteredText = taskExecutionInput.enteredText() != null;

		if (taskExecutionInput.executionStatus() != ChecklistTaskExecutionStatus.COMPLETED) {
			if (hasVerificationResult || hasMeasuredValue || hasEnteredText) {
				throw new ConflictException(
						"checklist_task_execution_pending_data_forbidden",
						"Only completed checklist tasks may record verification, measurement, or text data"
				);
			}
			return;
		}

		switch (taskExecution.getTaskKindSnapshot()) {
			case ACTION -> {
				if (hasVerificationResult || hasMeasuredValue || hasEnteredText) {
					throw new ConflictException(
							"checklist_task_execution_action_data_invalid",
							"Action checklist tasks may only record completion and an optional comment"
					);
				}
			}
			case VERIFICATION -> {
				if (!hasVerificationResult || hasMeasuredValue || hasEnteredText) {
					throw new ConflictException(
							"checklist_task_execution_verification_invalid",
							"Verification checklist tasks require a verification result and no measurement or text entry"
					);
				}
			}
			case MEASUREMENT -> {
				if (!hasMeasuredValue || hasVerificationResult || hasEnteredText) {
					throw new ConflictException(
							"checklist_task_execution_measurement_invalid",
							"Measurement checklist tasks require a measured value and no verification or text entry"
					);
				}
			}
			case TEXT_ENTRY -> {
				if (!hasEnteredText || hasVerificationResult || hasMeasuredValue) {
					throw new ConflictException(
							"checklist_task_execution_text_entry_invalid",
							"Text entry checklist tasks require entered text and no verification or measured value"
					);
				}
			}
		}
	}

	private void validateRequiredTaskExecutionsCompleted(List<ChecklistTaskExecution> taskExecutions) {
		taskExecutions.stream().filter(ChecklistTaskExecution::isRequired)
				.filter(taskExecution -> taskExecution.getExecutionStatus() != ChecklistTaskExecutionStatus.COMPLETED)
				.forEach(_ -> {
			throw new ConflictException(
					"checklist_run_required_task_incomplete",
					"All required checklist tasks must be completed before submission"
			);
		});
	}

	public record ChecklistTaskExecutionInput(
			UUID checklistTaskExecutionId,
			ChecklistTaskExecutionStatus executionStatus,
			String comment,
			ChecklistVerificationResult verificationResult,
			BigDecimal measuredValue,
			String enteredText
	) {
	}
}
