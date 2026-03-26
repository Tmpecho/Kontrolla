package org.kontrolla.checklists.api;

import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskExecution;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ChecklistRunResponse(
		UUID id,
		UUID checklistDefinitionId,
		UUID definitionGroupId,
		UUID establishmentId,
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		Instant dueAt,
		ChecklistRunStatus status,
		Instant startedAt,
		Instant completedAt,
		UUID completedByUserId,
		UUID createdByUserId,
		Instant createdAt,
		Instant updatedAt,
		List<ChecklistRunAssignmentResponse> assignments,
		List<ChecklistTaskExecutionResponse> tasks,
		List<ChecklistRunEventResponse> events
) {

	public static ChecklistRunResponse from(ChecklistRun checklistRun) {
		return new ChecklistRunResponse(
				checklistRun.getId(),
				checklistRun.getChecklistDefinition().getId(),
				checklistRun.getDefinitionGroupId(),
				checklistRun.getEstablishment().getId(),
				checklistRun.getServiceArea(),
				checklistRun.getTitleSnapshot(),
				checklistRun.getDescriptionSnapshot(),
				checklistRun.getDueAt(),
				checklistRun.getStatus(),
				checklistRun.getStartedAt(),
				checklistRun.getCompletedAt(),
				checklistRun.getCompletedByUser() == null ? null : checklistRun.getCompletedByUser().getId(),
				checklistRun.getCreatedByUser().getId(),
				checklistRun.getCreatedAt(),
				checklistRun.getUpdatedAt(),
				checklistRun.getAssignments().stream()
						.map(ChecklistRunAssignmentResponse::from)
						.toList(),
				checklistRun.getTaskExecutions().stream()
						.sorted(Comparator.comparingInt(ChecklistTaskExecution::getSortOrder))
						.map(ChecklistTaskExecutionResponse::from)
						.toList(),
				checklistRun.getEvents().stream()
						.sorted(Comparator.comparing(ChecklistRunEvent::getOccurredAt))
						.map(ChecklistRunEventResponse::from)
						.toList()
		);
	}

	public record ChecklistRunAssignmentResponse(
			UUID id,
			UUID assignedUserId,
			UUID assignedByUserId,
			Instant assignedAt
	) {

		private static ChecklistRunAssignmentResponse from(ChecklistRunAssignment assignment) {
			return new ChecklistRunAssignmentResponse(
					assignment.getId(),
					assignment.getAssignedUser().getId(),
					assignment.getAssignedByUser().getId(),
					assignment.getAssignedAt()
			);
		}
	}

	public record ChecklistTaskExecutionResponse(
			UUID checklistTaskExecutionId,
			UUID sourceChecklistTaskDefinitionId,
			String title,
			String details,
			ChecklistTaskKind taskKind,
			boolean required,
			int sortOrder,
			String measurementUnit,
			BigDecimal minimumAllowedValue,
			BigDecimal maximumAllowedValue,
			ChecklistTaskExecutionStatus executionStatus,
			Instant resolvedAt,
			UUID resolvedByUserId,
			String comment,
			ChecklistVerificationResult verificationResult,
			BigDecimal measuredValue,
			String enteredText
	) {

		private static ChecklistTaskExecutionResponse from(ChecklistTaskExecution taskExecution) {
			return new ChecklistTaskExecutionResponse(
					taskExecution.getId(),
					taskExecution.getSourceChecklistTaskDefinition() == null ? null : taskExecution.getSourceChecklistTaskDefinition().getId(),
					taskExecution.getTitleSnapshot(),
					taskExecution.getDetailsSnapshot(),
					taskExecution.getTaskKindSnapshot(),
					taskExecution.isRequired(),
					taskExecution.getSortOrder(),
					taskExecution.getMeasurementUnitSnapshot(),
					taskExecution.getMinimumAllowedValueSnapshot(),
					taskExecution.getMaximumAllowedValueSnapshot(),
					taskExecution.getExecutionStatus(),
					taskExecution.getResolvedAt(),
					taskExecution.getResolvedByUser() == null ? null : taskExecution.getResolvedByUser().getId(),
					taskExecution.getComment(),
					taskExecution.getVerificationResult(),
					taskExecution.getMeasuredValue(),
					taskExecution.getEnteredText()
			);
		}
	}

	public record ChecklistRunEventResponse(
			UUID id,
			ChecklistRunEventType eventType,
			UUID actorUserId,
			Instant occurredAt,
			String metadataJson
	) {

		private static ChecklistRunEventResponse from(ChecklistRunEvent event) {
			return new ChecklistRunEventResponse(
					event.getId(),
					event.getEventType(),
					event.getActorUser() == null ? null : event.getActorUser().getId(),
					event.getOccurredAt(),
					event.getMetadataJson()
			);
		}
	}
}
