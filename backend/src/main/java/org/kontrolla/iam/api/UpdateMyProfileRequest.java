package org.kontrolla.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the current user's profile.
 *
 * @param firstName the new first name
 * @param lastName the new last name
 */
public record UpdateMyProfileRequest(
    @NotBlank @Size(max = 255) String firstName, @NotBlank @Size(max = 255) String lastName) {}
