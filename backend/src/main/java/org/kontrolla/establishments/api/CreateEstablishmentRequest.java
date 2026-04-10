package org.kontrolla.establishments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;

/**
 * Request payload for creating an establishment.
 *
 * @param name the establishment name
 * @param type the establishment type
 * @param status the initial establishment status
 */
public record CreateEstablishmentRequest(
		@NotBlank @Size(max = 255) String name,
		@NotNull EstablishmentType type,
		EstablishmentStatus status
) {
}
