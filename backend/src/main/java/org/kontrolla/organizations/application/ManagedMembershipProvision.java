package org.kontrolla.organizations.application;

import java.time.Instant;
import org.kontrolla.organizations.domain.OrganizationMembership;

/**
 * Result of creating a managed organization member together with invite metadata.
 *
 * @param membership the created membership
 * @param inviteExpiresAt when the invite expires
 * @param inviteUrl the invitation URL
 */
public record ManagedMembershipProvision(
    OrganizationMembership membership, Instant inviteExpiresAt, String inviteUrl) {}
