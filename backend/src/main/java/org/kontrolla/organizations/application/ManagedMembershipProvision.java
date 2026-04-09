package org.kontrolla.organizations.application;

import org.kontrolla.organizations.domain.OrganizationMembership;

import java.time.Instant;

public record ManagedMembershipProvision(
		OrganizationMembership membership,
		Instant inviteExpiresAt,
		String inviteUrl
) {
}
