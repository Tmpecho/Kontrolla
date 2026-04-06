package org.kontrolla.iam.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InviteNotificationService {

	private static final Logger log = LoggerFactory.getLogger(InviteNotificationService.class);

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
