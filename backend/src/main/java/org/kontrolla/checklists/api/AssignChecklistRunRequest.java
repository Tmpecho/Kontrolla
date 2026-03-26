package org.kontrolla.checklists.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignChecklistRunRequest(
		@NotEmpty List<@NotNull UUID> assignedUserIds
) {
}
