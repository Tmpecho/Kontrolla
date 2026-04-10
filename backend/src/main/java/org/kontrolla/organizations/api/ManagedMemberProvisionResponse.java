package org.kontrolla.organizations.api;

import java.time.Instant;
import org.kontrolla.organizations.application.ManagedMembershipProvision;

/**
 * API response containing a created managed membership and invite details.
 *
 * @param membership the created membership response
 * @param inviteExpiresAt when the invite expires
 * @param inviteUrl the invitation URL
 */
public record ManagedMemberProvisionResponse(
    MembershipResponse membership, Instant inviteExpiresAt, String inviteUrl) {

  /**
   * Maps a managed membership provision result to the API response.
   *
   * @param provision the provision result to map
   * @return the mapped response
   */
  public static ManagedMemberProvisionResponse from(ManagedMembershipProvision provision) {
    return new ManagedMemberProvisionResponse(
        MembershipResponse.from(provision.membership()),
        provision.inviteExpiresAt(),
        provision.inviteUrl());
  }
}
