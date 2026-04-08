package org.kontrolla.iam.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "app.mail.invites", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingInviteNotificationService implements InviteNotificationService {

	private static final Logger log = LoggerFactory.getLogger(LoggingInviteNotificationService.class);

	@Override
	public void sendOrganizationMemberInvite(
			String recipientEmail,
			String organizationName,
			String inviteUrl,
			Instant expiresAt
	) {
		log.info(
				"Organization member invite prepared for {} in {}. Expires at {}. Invite URL: {}",
				recipientEmail,
				organizationName,
				expiresAt,
				inviteUrl
		);
	}
}
