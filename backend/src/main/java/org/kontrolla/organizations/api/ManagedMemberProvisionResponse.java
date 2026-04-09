package org.kontrolla.organizations.api;

import org.kontrolla.organizations.application.ManagedMembershipProvision;

import java.time.Instant;

public record ManagedMemberProvisionResponse(
		MembershipResponse membership,
		Instant inviteExpiresAt,
		String inviteUrl
) {

	public static ManagedMemberProvisionResponse from(ManagedMembershipProvision provision) {
		return new ManagedMemberProvisionResponse(
				MembershipResponse.from(provision.membership()),
				provision.inviteExpiresAt(),
				provision.inviteUrl()
		);
	}
}
