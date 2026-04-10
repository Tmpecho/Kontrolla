package org.kontrolla.iam.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Invite notification implementation that sends invitation emails through SMTP. */
@Service
@ConditionalOnProperty(prefix = "app.mail.invites", name = "enabled", havingValue = "true")
public class SmtpInviteNotificationService implements InviteNotificationService {

  private static final Logger log = LoggerFactory.getLogger(SmtpInviteNotificationService.class);

  private final JavaMailSender mailSender;
  private final MailInviteProperties mailInviteProperties;

  /**
   * Creates the SMTP invite notification service.
   *
   * @param mailSender mail sender used to deliver invite messages
   * @param mailInviteProperties invite mail configuration properties
   */
  public SmtpInviteNotificationService(
      JavaMailSender mailSender, MailInviteProperties mailInviteProperties) {
    this.mailSender = mailSender;
    this.mailInviteProperties = mailInviteProperties;
  }

  /**
   * Sends an organization member invite email.
   *
   * @param recipientEmail the invite recipient email
   * @param organizationName the invited organization name
   * @param inviteUrl the invite URL
   * @param expiresAt when the invite expires
   */
  @Override
  public void sendOrganizationMemberInvite(
      String recipientEmail, String organizationName, String inviteUrl, Instant expiresAt) {
    mailSender.send(prepareMessage(recipientEmail, organizationName, inviteUrl, expiresAt));
    log.info(
        "Organization member invite emailed to {} for {}. Expires at {}.",
        recipientEmail,
        organizationName,
        expiresAt);
  }

  private MimeMessage prepareMessage(
      String recipientEmail, String organizationName, String inviteUrl, Instant expiresAt) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setTo(recipientEmail);
      helper.setFrom(senderAddress());
      helper.setSubject("You have been invited to " + organizationName + " on Kontrolla");
      helper.setText(buildMessageBody(organizationName, inviteUrl, expiresAt), false);
      return message;
    } catch (MessagingException exception) {
      throw new MailPreparationException("Failed to prepare invite email", exception);
    }
  }

  private InternetAddress senderAddress() {
    String fromAddress = mailInviteProperties.getFromAddress();
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalStateException(
          "app.mail.invites.from-address must be configured when invite email delivery is enabled");
    }
    try {
      return new InternetAddress(
          fromAddress, mailInviteProperties.getFromName(), StandardCharsets.UTF_8.name());
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid invite sender address configuration", exception);
    }
  }

  private String buildMessageBody(String organizationName, String inviteUrl, Instant expiresAt) {
    return """
				You have been invited to join %s on Kontrolla.

				Accept your invitation by opening this link:
				%s

				This invitation expires at %s.
				"""
        .formatted(organizationName, inviteUrl, expiresAt);
  }
}
