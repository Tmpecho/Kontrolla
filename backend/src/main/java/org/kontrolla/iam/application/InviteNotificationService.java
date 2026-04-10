package org.kontrolla.iam.application;

import java.time.Instant;

/** Sends invitation notifications for organization member onboarding. */
public interface InviteNotificationService {

  /**
   * Sends an organization member invite notification.
   *
   * @param recipientEmail the invite recipient email
   * @param organizationName the invited organization name
   * @param inviteUrl the invite URL
   * @param expiresAt when the invite expires
   */
  void sendOrganizationMemberInvite(
      String recipientEmail, String organizationName, String inviteUrl, Instant expiresAt);
}
