package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for adding a timeline note to a deviation.
 *
 * @param note the note to append to the timeline
 */
public record AddDeviationTimelineNoteRequest(
		@NotBlank @Size(max = 2000) String note
) {
}
