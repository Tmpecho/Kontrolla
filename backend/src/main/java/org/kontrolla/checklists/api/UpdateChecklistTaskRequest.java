package org.kontrolla.checklists.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;
import java.math.BigDecimal;

/**
 * Request payload for updating a single checklist task execution.
 *
 * @param executionStatus execution status selected for the task
 * @param comment optional task comment
 * @param verificationResult verification result for verification tasks
 * @param measuredValue measured value for measurement tasks
 * @param enteredText entered text for text-entry tasks
 */
public record UpdateChecklistTaskRequest(
        @NotNull ChecklistTaskExecutionStatus executionStatus,
        String comment,
        ChecklistVerificationResult verificationResult,
        BigDecimal measuredValue,
        String enteredText
) {}
