package org.kontrolla.deviations.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for assigning a deviation to a user.
 *
 * @param assignedUserId the user to assign to the deviation
 */
public record AssignDeviationRequest(@NotNull UUID assignedUserId) {}
