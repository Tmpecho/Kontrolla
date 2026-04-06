package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.deviations.domain.DeviationStatus;

public record UpdateDeviationStatusRequest(
		@NotNull DeviationStatus status
) {
}
