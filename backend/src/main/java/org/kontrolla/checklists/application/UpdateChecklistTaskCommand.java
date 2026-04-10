package org.kontrolla.checklists.application;

import java.math.BigDecimal;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

/**
 * Command for updating a single checklist task execution.
 *
 * @param executionStatus the new execution status
 * @param comment the task comment
 * @param verificationResult the verification result, if applicable
 * @param measuredValue the measured value, if applicable
 * @param enteredText the entered text, if applicable
 */
public record UpdateChecklistTaskCommand(
    ChecklistTaskExecutionStatus executionStatus,
    String comment,
    ChecklistVerificationResult verificationResult,
    BigDecimal measuredValue,
    String enteredText) {}
