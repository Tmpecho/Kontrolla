package org.kontrolla.organizations.application;

import org.kontrolla.organizations.domain.OrganizationMembership;

import java.time.Instant;

/**
 * Result of creating a managed organization member together with invite
 * metadata.
 *
 * @param membership the created membership
 * @param inviteExpiresAt when the invite expires
 * @param inviteUrl the invitation URL
 */
public record ManagedMembershipProvision(
		OrganizationMembership membership,
		Instant inviteExpiresAt,
		String inviteUrl
) {
}
