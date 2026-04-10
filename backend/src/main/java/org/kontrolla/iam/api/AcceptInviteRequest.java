package org.kontrolla.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for accepting an invitation.
 *
 * @param password the password chosen while accepting the invite
 */
public record AcceptInviteRequest(@NotBlank @Size(min = 8, max = 200) String password) {}
