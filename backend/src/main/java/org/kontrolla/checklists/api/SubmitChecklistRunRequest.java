package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubmitChecklistRunRequest(
		@NotEmpty List<@Valid ChecklistTaskExecutionRequest> tasks
) {

	public record ChecklistTaskExecutionRequest(
			@NotNull UUID checklistTaskExecutionId,
			@NotNull ChecklistTaskExecutionStatus executionStatus,
			@Size(max = 1000) String comment,
			ChecklistVerificationResult verificationResult,
			BigDecimal measuredValue,
			@Size(max = 2000) String enteredText
	) {
	}
}
