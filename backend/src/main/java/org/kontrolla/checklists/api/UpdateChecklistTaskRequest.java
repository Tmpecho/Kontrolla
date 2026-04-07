package org.kontrolla.checklists.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;
import java.math.BigDecimal;

public record UpdateChecklistTaskRequest(
        @NotNull ChecklistTaskExecutionStatus executionStatus,
        String comment,
        ChecklistVerificationResult verificationResult,
        BigDecimal measuredValue,
        String enteredText
) {}
