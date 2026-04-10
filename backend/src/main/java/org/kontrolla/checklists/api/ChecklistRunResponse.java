package org.kontrolla.checklists.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.kontrolla.iam.domain.User;

/**
 * Response payload describing a checklist run together with its assignments, tasks, and events.
 *
 * @param id identifier of the checklist run
 * @param checklistDefinitionId source checklist definition identifier
 * @param definitionGroupId identifier shared across definition versions
 * @param establishmentId establishment that owns the run
 * @param serviceArea service area covered by the run
 * @param title title snapshot taken when the run was created
 * @param description description snapshot taken when the run was created
 * @param dueAt due timestamp for the run
 * @param status current run status
 * @param startedAt instant when the run was started
 * @param completedAt instant when the run was completed
 * @param completedByUserId user who completed the run
 * @param createdByUserId user who created the run
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param assignments current run assignments
 * @param tasks task executions belonging to the run
 * @param events audit events recorded for the run
 */
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
    List<ChecklistRunEventResponse> events) {

  /**
   * Maps a checklist run entity to an API response.
   *
   * @param checklistRun checklist run entity to convert
   * @return the mapped response
   */
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
        checklistRun.getCompletedByUser() == null
            ? null
            : checklistRun.getCompletedByUser().getId(),
        checklistRun.getCreatedByUser().getId(),
        checklistRun.getCreatedAt(),
        checklistRun.getUpdatedAt(),
        distinctById(checklistRun.getAssignments(), ChecklistRunAssignment::getId).stream()
            .map(ChecklistRunAssignmentResponse::from)
            .toList(),
        distinctById(checklistRun.getTaskExecutions(), ChecklistTaskExecution::getId).stream()
            .sorted(Comparator.comparingInt(ChecklistTaskExecution::getSortOrder))
            .map(ChecklistTaskExecutionResponse::from)
            .toList(),
        distinctById(checklistRun.getEvents(), ChecklistRunEvent::getId).stream()
            .sorted(Comparator.comparing(ChecklistRunEvent::getOccurredAt))
            .map(ChecklistRunEventResponse::from)
            .toList());
  }

  private static <T> List<T> distinctById(
      Iterable<T> items, java.util.function.Function<T, UUID> idExtractor) {
    Map<UUID, T> itemsById = new LinkedHashMap<>();

    for (T item : items) {
      itemsById.putIfAbsent(idExtractor.apply(item), item);
    }

    return List.copyOf(itemsById.values());
  }

  /**
   * Response payload describing a user assignment on a checklist run.
   *
   * @param id identifier of the assignment
   * @param assignedUserId identifier of the assigned user
   * @param assignedUserName display name of the assigned user
   * @param assignedByUserId identifier of the user who made the assignment
   * @param assignedAt assignment timestamp
   */
  public record ChecklistRunAssignmentResponse(
      UUID id,
      UUID assignedUserId,
      String assignedUserName,
      UUID assignedByUserId,
      Instant assignedAt) {

    private static ChecklistRunAssignmentResponse from(ChecklistRunAssignment assignment) {
      return new ChecklistRunAssignmentResponse(
          assignment.getId(),
          assignment.getAssignedUser().getId(),
          formatUserDisplayName(assignment.getAssignedUser()),
          assignment.getAssignedByUser().getId(),
          assignment.getAssignedAt());
    }
  }

  private static String formatUserDisplayName(User user) {
    return "%s %s".formatted(user.getFirstName(), user.getLastName()).trim();
  }

  /**
   * Response payload describing the execution state of a single checklist task.
   *
   * @param checklistTaskExecutionId identifier of the task execution
   * @param sourceChecklistTaskDefinitionId identifier of the source task definition
   * @param title task title snapshot
   * @param details task details snapshot
   * @param taskKind type of task to perform
   * @param required whether the task is required
   * @param sortOrder display order of the task
   * @param measurementUnit measurement unit for measurement tasks
   * @param minimumAllowedValue lower accepted measured value
   * @param maximumAllowedValue upper accepted measured value
   * @param executionStatus current execution status
   * @param resolvedAt instant when the task was resolved
   * @param resolvedByUserId user who resolved the task
   * @param comment comment entered for the task
   * @param verificationResult verification outcome for verification tasks
   * @param measuredValue measured value for measurement tasks
   * @param enteredText entered text for text-entry tasks
   */
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
      String enteredText) {

    private static ChecklistTaskExecutionResponse from(ChecklistTaskExecution taskExecution) {
      return new ChecklistTaskExecutionResponse(
          taskExecution.getId(),
          taskExecution.getSourceChecklistTaskDefinition() == null
              ? null
              : taskExecution.getSourceChecklistTaskDefinition().getId(),
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
          taskExecution.getResolvedByUser() == null
              ? null
              : taskExecution.getResolvedByUser().getId(),
          taskExecution.getComment(),
          taskExecution.getVerificationResult(),
          taskExecution.getMeasuredValue(),
          taskExecution.getEnteredText());
    }
  }

  /**
   * Response payload describing a lifecycle event recorded for a checklist run.
   *
   * @param id identifier of the event
   * @param eventType recorded event type
   * @param actorUserId user who triggered the event
   * @param occurredAt event timestamp
   * @param metadataJson serialized event metadata
   */
  public record ChecklistRunEventResponse(
      UUID id,
      ChecklistRunEventType eventType,
      UUID actorUserId,
      Instant occurredAt,
      String metadataJson) {

    private static ChecklistRunEventResponse from(ChecklistRunEvent event) {
      return new ChecklistRunEventResponse(
          event.getId(),
          event.getEventType(),
          event.getActorUser() == null ? null : event.getActorUser().getId(),
          event.getOccurredAt(),
          event.getMetadataJson());
    }
  }
}
