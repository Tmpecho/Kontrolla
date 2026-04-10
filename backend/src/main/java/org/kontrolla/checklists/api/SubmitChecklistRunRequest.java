package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

/**
 * Request payload for submitting task execution results for a checklist run.
 *
 * @param tasks task execution updates included in the submission
 */
public record SubmitChecklistRunRequest(
    @NotEmpty List<@Valid ChecklistTaskExecutionRequest> tasks) {

  /**
   * Request payload describing the submitted state of a checklist task execution.
   *
   * @param checklistTaskExecutionId identifier of the task execution to update
   * @param executionStatus execution status selected for the task
   * @param comment optional task comment
   * @param verificationResult verification result for verification tasks
   * @param measuredValue measured value for measurement tasks
   * @param enteredText entered text for text-entry tasks
   */
  public record ChecklistTaskExecutionRequest(
      @NotNull UUID checklistTaskExecutionId,
      @NotNull ChecklistTaskExecutionStatus executionStatus,
      @Size(max = 1000) String comment,
      ChecklistVerificationResult verificationResult,
      BigDecimal measuredValue,
      @Size(max = 2000) String enteredText) {}
}
