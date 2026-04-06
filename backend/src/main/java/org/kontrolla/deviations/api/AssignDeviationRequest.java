package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDeviationRequest(
		@NotNull UUID assignedUserId
) {
}
