package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

import java.math.BigDecimal;
import java.util.UUID;

public record ChecklistRunTaskExecutionInput(
		UUID checklistTaskExecutionId,
		ChecklistTaskExecutionStatus executionStatus,
		String comment,
		ChecklistVerificationResult verificationResult,
		BigDecimal measuredValue,
		String enteredText
) {
}
