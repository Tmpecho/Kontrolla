package org.kontrolla.checklists.api;

import org.kontrolla.checklists.domain.ChecklistItemDefinition;
import org.kontrolla.checklists.domain.ChecklistItemResponse;
import org.kontrolla.checklists.domain.ChecklistResponseType;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ChecklistRunResponse(
		UUID id,
		UUID checklistDefinitionId,
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
		List<ChecklistRunItemResponse> items,
		List<ChecklistRunEventResponse> events
) {

	public static ChecklistRunResponse from(ChecklistRun checklistRun) {
		Map<UUID, ChecklistItemResponse> responsesByItemId = checklistRun.getItemResponses().stream()
				.collect(Collectors.toMap(response -> response.getChecklistItemDefinition().getId(), Function.identity()));

		return new ChecklistRunResponse(
				checklistRun.getId(),
				checklistRun.getChecklistDefinition().getId(),
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
				checklistRun.getChecklistDefinition().getItems().stream()
						.sorted(Comparator.comparingInt(ChecklistItemDefinition::getSortOrder))
						.map(item -> ChecklistRunItemResponse.from(item, responsesByItemId.get(item.getId())))
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

	public record ChecklistRunItemResponse(
			UUID checklistItemDefinitionId,
			String prompt,
			String instructionText,
			ChecklistResponseType responseType,
			boolean required,
			int sortOrder,
			Boolean booleanValue,
			String textValue,
			BigDecimal numberValue,
			String note
	) {

		private static ChecklistRunItemResponse from(ChecklistItemDefinition item, ChecklistItemResponse response) {
			return new ChecklistRunItemResponse(
					item.getId(),
					item.getPrompt(),
					item.getInstructionText(),
					item.getResponseType(),
					item.isRequired(),
					item.getSortOrder(),
					response == null ? null : response.getBooleanValue(),
					response == null ? null : response.getTextValue(),
					response == null ? null : response.getNumberValue(),
					response == null ? null : response.getNote()
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
