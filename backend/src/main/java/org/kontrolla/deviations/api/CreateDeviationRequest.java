package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationSeverity;

/**
 * Request payload for creating a deviation.
 *
 * @param title the deviation title
 * @param description the deviation description
 * @param category the deviation category
 * @param severity the deviation severity
 */
public record CreateDeviationRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 2000) String description,
    @NotNull DeviationCategory category,
    @NotNull DeviationSeverity severity) {}
