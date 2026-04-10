package org.kontrolla.iam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.organizations.domain.Organization;

import java.time.Instant;

/**
 * Persisted invitation token that allows a user to join an organization.
 */
@Getter
@Entity
@Table(name = "user_invites")
public class UserInvite extends AbstractAuditableUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	protected UserInvite() {
	}

	/**
	 * Creates a user invite.
	 *
	 * @param user the invited user
	 * @param organization the invited organization
	 * @param tokenHash the hashed invite token
	 * @param expiresAt when the invite expires
	 */
	public UserInvite(User user, Organization organization, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.organization = organization;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	/**
	 * Indicates whether the invite is still active at a given instant.
	 *
	 * @param instant the instant to evaluate
	 * @return {@code true} when the invite is unaccepted and unexpired
	 */
	public boolean isActiveAt(Instant instant) {
		return acceptedAt == null && expiresAt.isAfter(instant);
	}

	/**
	 * Marks the invite as accepted.
	 *
	 * @param instant the acceptance timestamp
	 */
	public void accept(Instant instant) {
		this.acceptedAt = instant;
	}
}
