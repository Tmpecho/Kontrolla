package org.kontrolla.iam.api;

import org.kontrolla.iam.application.InviteDetails;

import java.time.Instant;

public record InviteDetailsResponse(
		String email,
		String firstName,
		String lastName,
		String organizationName,
		Instant expiresAt
) {

	public static InviteDetailsResponse from(InviteDetails inviteDetails) {
		return new InviteDetailsResponse(
				inviteDetails.email(),
				inviteDetails.firstName(),
				inviteDetails.lastName(),
				inviteDetails.organizationName(),
				inviteDetails.expiresAt()
		);
	}
}
