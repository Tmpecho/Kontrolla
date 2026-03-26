package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubmitChecklistRunRequest(
		@NotEmpty List<@Valid ChecklistItemSubmissionRequest> responses
) {

	public record ChecklistItemSubmissionRequest(
			@NotNull UUID checklistItemDefinitionId,
			Boolean booleanValue,
			@Size(max = 2000) String textValue,
			BigDecimal numberValue,
			@Size(max = 1000) String note
	) {
	}
}
