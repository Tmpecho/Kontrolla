package org.kontrolla.checklists.application;

import java.math.BigDecimal;
import java.util.UUID;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

/**
 * Application input for submitting the state of one checklist task execution.
 *
 * @param checklistTaskExecutionId the task execution identifier
 * @param executionStatus the execution status
 * @param comment the task comment
 * @param verificationResult the verification result, if applicable
 * @param measuredValue the measured value, if applicable
 * @param enteredText the entered text, if applicable
 */
public record ChecklistRunTaskExecutionInput(
    UUID checklistTaskExecutionId,
    ChecklistTaskExecutionStatus executionStatus,
    String comment,
    ChecklistVerificationResult verificationResult,
    BigDecimal measuredValue,
    String enteredText) {}
