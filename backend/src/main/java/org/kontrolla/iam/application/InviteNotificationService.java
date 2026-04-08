package org.kontrolla.iam.application;

import java.time.Instant;

public interface InviteNotificationService {

	void sendOrganizationMemberInvite(
			String recipientEmail,
			String organizationName,
			String inviteUrl,
			Instant expiresAt
	);
}
