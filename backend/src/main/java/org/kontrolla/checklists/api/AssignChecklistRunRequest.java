package org.kontrolla.checklists.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for assigning one or more users to a checklist run.
 *
 * @param assignedUserIds identifiers of the users to assign
 */
public record AssignChecklistRunRequest(
		@NotEmpty List<@NotNull UUID> assignedUserIds
) {
}
