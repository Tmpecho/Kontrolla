package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddDeviationTimelineNoteRequest(
		@NotBlank @Size(max = 2000) String note
) {
}
