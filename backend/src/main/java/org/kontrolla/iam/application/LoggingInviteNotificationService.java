package org.kontrolla.iam.application;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Invite notification implementation that logs invite details instead of sending email. */
@Service
@ConditionalOnProperty(
    prefix = "app.mail.invites",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingInviteNotificationService implements InviteNotificationService {

  private static final Logger log = LoggerFactory.getLogger(LoggingInviteNotificationService.class);

  /**
   * Logs an organization member invite for development or fallback operation.
   *
   * @param recipientEmail the invite recipient email
   * @param organizationName the invited organization name
   * @param inviteUrl the invite URL
   * @param expiresAt when the invite expires
   */
  @Override
  public void sendOrganizationMemberInvite(
      String recipientEmail, String organizationName, String inviteUrl, Instant expiresAt) {
    log.info(
        "Organization member invite prepared for {} in {}. Expires at {}. Invite URL: {}",
        recipientEmail,
        organizationName,
        expiresAt,
        inviteUrl);
  }
}
