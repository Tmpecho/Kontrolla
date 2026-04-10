package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.deviations.domain.DeviationStatus;

/**
 * Request payload for updating the status of a deviation.
 *
 * @param status the new deviation status
 */
public record UpdateDeviationStatusRequest(
		@NotNull DeviationStatus status
) {
}
